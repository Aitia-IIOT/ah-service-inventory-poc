package eu.arrowhead.core.mscv.service;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.CoreCommonConstants;
import eu.arrowhead.common.CoreEventHandlerConstants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.database.entity.mscv.SshTarget;
import eu.arrowhead.common.database.entity.mscv.Target;
import eu.arrowhead.common.database.entity.mscv.VerificationEntryList;
import eu.arrowhead.common.drivers.EventDriver;
import eu.arrowhead.common.dto.shared.DeviceRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.DeviceRequestDTO;
import eu.arrowhead.common.dto.shared.EventDTO;
import eu.arrowhead.common.dto.shared.ServiceRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.SubscriptionRequestDTO;
import eu.arrowhead.common.dto.shared.SystemRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.common.dto.shared.mscv.Layer;
import eu.arrowhead.core.mscv.MscvDefaults;
import eu.arrowhead.core.mscv.quartz.VerificationJobFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static eu.arrowhead.common.CommonConstants.OP_MSCV_PUBLISH_URI;

@Service
@Api(tags = {CoreCommonConstants.SWAGGER_TAG_ALL})
@CrossOrigin(maxAge = Defaults.CORS_MAX_AGE, allowCredentials = Defaults.CORS_ALLOW_CREDENTIALS,
        allowedHeaders = {HttpHeaders.ORIGIN, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT}
)
@RestController
@RequestMapping(CommonConstants.MSCV_URI + CoreCommonConstants.MGMT_URI)
public class EventHandlerListener {

    private static final int MAX_ATTEMPTS = 5;
    private final Logger logger = LogManager.getLogger();

    private final MscvDefaults mscvDefaults;
    private final MscvExecutionService executionService;
    private final MscvTargetService targetService;
    private final VerificationJobFactory jobFactory;
    private final EventDriver eventDriver;

    @Autowired
    public EventHandlerListener(final MscvDefaults mscvDefaults,
                                final MscvExecutionService executionService,
                                final MscvTargetService targetService,
                                final VerificationJobFactory jobFactory,
                                final EventDriver eventDriver) {
        super();
        this.mscvDefaults = mscvDefaults;
        this.executionService = executionService;
        this.targetService = targetService;
        this.jobFactory = jobFactory;
        this.eventDriver = eventDriver;
    }

    //-------------------------------------------------------------------------------------------------
    @ApiOperation(value = "Event Handler callback controller", tags = {CoreCommonConstants.SWAGGER_TAG_MGMT})
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.SC_OK, message = "Success"),
            @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = "Bad Request"),
            @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
            @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
    })
    @PostMapping(OP_MSCV_PUBLISH_URI)
    @ResponseBody
    public void publish(@RequestBody final EventDTO event) throws IOException, MscvException, SchedulerException {
        logger.debug("publish started ...");

        switch (event.getEventType()) {
            case CoreEventHandlerConstants.REGISTER_DEVICE_EVENT:
                final var registerDevice = eventDriver.convert(event.getPayload(), DeviceRegistryRequestDTO.class);
                createJob(convert(registerDevice.getDevice()), Layer.DEVICE);
                break;
            case CoreEventHandlerConstants.UNREGISTER_DEVICE_EVENT:
                final var unregisterDevice = eventDriver.convert(event.getPayload(), DeviceRegistryRequestDTO.class);
                deleteJob(convert(unregisterDevice.getDevice()), Layer.DEVICE);
                break;
            case CoreEventHandlerConstants.REGISTER_SYSTEM_EVENT:
                final var registerSystem = eventDriver.convert(event.getPayload(), SystemRegistryRequestDTO.class);
                createJob(convert(registerSystem.getSystem()), Layer.SYSTEM);
                break;
            case CoreEventHandlerConstants.UNREGISTER_SYSTEM_EVENT:
                final var unregisterSystem = eventDriver.convert(event.getPayload(), SystemRegistryRequestDTO.class);
                deleteJob(convert(unregisterSystem.getSystem()), Layer.SYSTEM);
                break;
            case CoreEventHandlerConstants.REGISTER_SERVICE_EVENT:
                final var registerService = eventDriver.convert(event.getPayload(), ServiceRegistryRequestDTO.class);
                createJob(convert(registerService.getProviderSystem()), Layer.SERVICE);
                break;
            case CoreEventHandlerConstants.UNREGISTER_SERVICE_EVENT:
                final var unregisterService = eventDriver.convert(event.getPayload(), ServiceRegistryRequestDTO.class);
                deleteJob(convert(unregisterService.getProviderSystem()), Layer.SERVICE);
                break;
            default:
                logger.warn("Unknown event type {} with payload: {}", event.getEventType(), event.getPayload());
        }
    }

    @EventListener
    @Order(99) // can't use @PostConstruct because authorization rules are done be ApplicationInitListener
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        unsubscribe();
        subscribe();
    }

    public void subscribe() {
        boolean success = false;
        int count = 0;

        do {
            try {
                eventDriver.subscribe(createSubscriptionRequest(CoreEventHandlerConstants.REGISTER_DEVICE_EVENT));
                eventDriver.subscribe(createSubscriptionRequest(CoreEventHandlerConstants.UNREGISTER_DEVICE_EVENT));
                eventDriver.subscribe(createSubscriptionRequest(CoreEventHandlerConstants.REGISTER_SYSTEM_EVENT));
                eventDriver.subscribe(createSubscriptionRequest(CoreEventHandlerConstants.UNREGISTER_SYSTEM_EVENT));
                eventDriver.subscribe(createSubscriptionRequest(CoreEventHandlerConstants.REGISTER_SERVICE_EVENT));
                eventDriver.subscribe(createSubscriptionRequest(CoreEventHandlerConstants.UNREGISTER_SERVICE_EVENT));
                success = true;
            } catch (final Exception e) {
                logger.warn("Unable to subscribe to Event Handler: {}", e.getMessage());
                sleep(TimeUnit.SECONDS.toMillis(15));
            } finally {
                count++;
            }
        } while (!success && count <= MAX_ATTEMPTS);
    }

    @PreDestroy
    public void unsubscribe() {
        final SystemRequestDTO requester = eventDriver.getRequesterSystem();
        try {
            eventDriver.unsubscribe(CoreEventHandlerConstants.REGISTER_DEVICE_EVENT, requester.getSystemName(), requester.getAddress(), requester.getPort());
            eventDriver.unsubscribe(CoreEventHandlerConstants.UNREGISTER_DEVICE_EVENT, requester.getSystemName(), requester.getAddress(), requester.getPort());
            eventDriver.unsubscribe(CoreEventHandlerConstants.REGISTER_SYSTEM_EVENT, requester.getSystemName(), requester.getAddress(), requester.getPort());
            eventDriver.unsubscribe(CoreEventHandlerConstants.UNREGISTER_SYSTEM_EVENT, requester.getSystemName(), requester.getAddress(), requester.getPort());
            eventDriver.unsubscribe(CoreEventHandlerConstants.REGISTER_SERVICE_EVENT, requester.getSystemName(), requester.getAddress(), requester.getPort());
            eventDriver.unsubscribe(CoreEventHandlerConstants.UNREGISTER_SERVICE_EVENT, requester.getSystemName(), requester.getAddress(), requester.getPort());
        } catch (final Exception e) {
            logger.warn("Unable to unsubscribe from Event Handler: {}", e.getMessage());
        }
    }

    private void createJob(final SshTarget sshTarget, final Layer layer) throws MscvException, SchedulerException {
        final Target target = targetService.findOrCreate(sshTarget);
        final VerificationEntryList entryList = executionService.findSuitableList(target, layer);
        jobFactory.createVerificationJob(entryList, target);
    }

    private void deleteJob(final SshTarget sshTarget, final Layer layer) throws MscvException {
        final Target target = targetService.findOrCreate(sshTarget);
        final VerificationEntryList entryList = executionService.findSuitableList(target, layer);
        jobFactory.removeVerificationJob(entryList, target);
    }


    private SubscriptionRequestDTO createSubscriptionRequest(final String eventType) {
        final SubscriptionRequestDTO dto = new SubscriptionRequestDTO();
        dto.setSubscriberSystem(eventDriver.getRequesterSystem());
        dto.setEventType(eventType);
        dto.setMatchMetaData(false);
        dto.setNotifyUri(CommonConstants.MSCV_URI + CoreCommonConstants.MGMT_URI + OP_MSCV_PUBLISH_URI);
        return dto;
    }

    private void sleep(final long sleepTime) {
        try {
            logger.info("Sleeping for {}s", TimeUnit.MILLISECONDS.toSeconds(sleepTime));
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    private SshTarget convert(final DeviceRequestDTO dto) {
        return new SshTarget(dto.getDeviceName(), mscvDefaults.getOs(), dto.getAddress(), mscvDefaults.getSshPort());
    }

    private SshTarget convert(final SystemRequestDTO dto) {
        return new SshTarget(dto.getSystemName(), mscvDefaults.getOs(), dto.getAddress(), mscvDefaults.getSshPort());
    }
}
