package eu.arrowhead.core.mscv.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import eu.arrowhead.common.database.entity.mscv.Script;
import eu.arrowhead.common.database.entity.mscv.SshTarget;
import eu.arrowhead.common.database.entity.mscv.Target;
import eu.arrowhead.common.database.entity.mscv.VerificationResult;
import eu.arrowhead.common.database.entity.mscv.VerificationResultDetail;
import eu.arrowhead.common.dto.shared.mscv.DetailSuccessIndicator;
import eu.arrowhead.common.dto.shared.mscv.OS;
import eu.arrowhead.common.dto.shared.mscv.SuccessIndicator;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.core.mscv.MscvDefaults;
import eu.arrowhead.core.mscv.security.KeyPairFileStorage;
import eu.arrowhead.core.mscv.service.MscvException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.scp.ScpClient;
import org.apache.sshd.client.scp.ScpClientCreator;
import org.apache.sshd.client.session.ClientSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import static eu.arrowhead.core.mscv.MscvMain.MSCV_EXECUTOR_SERVICE;
import static eu.arrowhead.core.mscv.Validation.TARGET_NULL_ERROR_MESSAGE;
import static eu.arrowhead.core.mscv.Validation.USERNAME_NULL_ERROR_MESSAGE;

@Component
public final class SshExecutionHandler implements ExecutionHandler<SshTarget> {

    private static final String AUTHORIZED_KEYS_FILE = "~/.ssh/authorized_keys";
    private static final int MAX_CONCURRENCY = 4;

    private final Logger logger = LogManager.getLogger();
    private final MscvDefaults mscvDefaults;
    private final UpdatingAcceptAllKeyVerifier acceptAllKeyVerifier;
    private final KeyPairFileStorage keyPairFileStorage;
    private final CompletionService<Queue<VerificationResultDetail>> completionService;
    private final SshClient sshClient;

    @Autowired
    public SshExecutionHandler(final MscvDefaults mscvDefaults,
                               final UpdatingAcceptAllKeyVerifier acceptAllKeyVerifier,
                               final KeyPairFileStorage keyPairFileStorage,
                               @Qualifier(MSCV_EXECUTOR_SERVICE) final ExecutorService executorService,
                               final SshClient sshClient) {
        super();
        logger.info("Creating {} with script base paths in {}", getClass().getSimpleName(), mscvDefaults.getDefaultPath());
        this.mscvDefaults = mscvDefaults;
        this.acceptAllKeyVerifier = acceptAllKeyVerifier;
        this.keyPairFileStorage = keyPairFileStorage;
        this.completionService = new ExecutorCompletionService(executorService);
        this.sshClient = sshClient;
    }

    @Override
    public void login(final SshTarget sshTarget, final String username, @Nullable final String password) throws AuthException {

        Assert.notNull(sshTarget, TARGET_NULL_ERROR_MESSAGE);
        Assert.hasText(username, USERNAME_NULL_ERROR_MESSAGE);
        final MscvDefaults.SshDefaults sshDefaults = mscvDefaults.getSsh();

        try (final ClientSession session = sshClient.connect(username, sshTarget.getAddress(), sshTarget.getPort())
                                                    .verify(sshDefaults.getConnectTimeout(), TimeUnit.SECONDS)
                                                    .getClientSession()) {

            session.addPasswordIdentity(password);
            session.setServerKeyVerifier(acceptAllKeyVerifier);

            session.auth().verify(sshDefaults.getAuthTimeout(), TimeUnit.SECONDS);
            copySshIdentity(session);
            session.disconnect(0, "exit");

        } catch (final Throwable e) {
            logger.error("Unable to login to {} with user {} and provided password. Reason: {}: {}",
                         sshTarget, username, e.getClass().getSimpleName(), e.getMessage());
            throw new AuthException("SSH login failure: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @Override
    public boolean verifyPasswordlessLogin(final SshTarget sshTarget) {
        Assert.notNull(sshTarget, TARGET_NULL_ERROR_MESSAGE);
        final MscvDefaults.SshDefaults sshDefaults = mscvDefaults.getSsh();

        try (final ClientSession session = sshClient.connect(sshTarget.getUsername(), sshTarget.getAddress(), sshTarget.getPort())
                                                    .verify(sshDefaults.getConnectTimeout(), TimeUnit.SECONDS)
                                                    .getClientSession()) {

            session.auth().verify(sshDefaults.getAuthTimeout(), TimeUnit.SECONDS);
            session.disconnect(0, "exit");

        } catch (final Throwable e) {
            logger.error("Unable to login to {} with user {} and provided password. Reason: {}: {}",
                         sshTarget, sshTarget.getUsername(), e.getClass().getSimpleName(), e.getMessage());
            throw new AuthException("SSH login failure: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return true;
    }

    public void performVerification(final VerificationResult intermediateResult, final Collection<VerificationResultDetail> resultDetails)
            throws MscvException {

        Assert.notNull(intermediateResult, "VerificationResult must not be null");
        Assert.notEmpty(resultDetails, "List of VerificationResultDetail must not be empty");

        final Target target;
        final SshTarget sshTarget;
        final Queue<VerificationResultDetail> detailsQueue;
        final ScpClient scpClient;
        final List<VerificationResultDetail> results;
        final MscvDefaults.SshDefaults sshDefaults = mscvDefaults.getSsh();


        target = intermediateResult.getTarget();
        if (!getType().isAssignableFrom(target.getClass())) {
            intermediateResult.setResult(SuccessIndicator.ERROR);
            resultDetails.forEach(r -> r.setResult(DetailSuccessIndicator.NOT_APPLICABLE));
            return;
        }

        sshTarget = (SshTarget) target;
        detailsQueue = new ArrayDeque<>(resultDetails);
        results = new ArrayList<>();

        try (final ClientSession session = sshClient.connect(sshTarget.getUsername(), sshTarget.getAddress(), sshTarget.getPort())
                                                    .verify(sshDefaults.getConnectTimeout(), TimeUnit.SECONDS)
                                                    .getClientSession()) {

            session.auth().verify(sshDefaults.getAuthTimeout(), TimeUnit.SECONDS);

            final ScpClientCreator creator = ScpClientCreator.instance();
            scpClient = creator.createScpClient(session);


            for (int i = 0; i < MAX_CONCURRENCY; i++) {
                completionService.submit(new QueueRunner(detailsQueue, scpClient, sshTarget.getOs()));
            }

            for (int i = 0; i < MAX_CONCURRENCY; i++) {
                final Future<Queue<VerificationResultDetail>> poll = completionService.take();
                results.addAll(poll.get());
            }

            session.disconnect(0, "exit");
        } catch (final Throwable e) {
            intermediateResult.setResult(SuccessIndicator.ERROR);
            throw new MscvException("Error during verification", e);
        }

        resultDetails.clear();
        resultDetails.addAll(results);
        intermediateResult.setResult(SuccessIndicator.SUCCESS);
    }

    @Override
    public Class<SshTarget> getType() {
        return SshTarget.class;
    }

    private CommandResult executeCommand(final ClientSession session, final String command) throws IOException {
        final MscvDefaults.SshDefaults sshDefaults = mscvDefaults.getSsh();
        final Duration timeout = Duration.ofSeconds(sshDefaults.getConnectTimeout());

        try (final ChannelExec channel = session.createExecChannel(command);
             final BufferedReader readFromHost = new BufferedReader(new InputStreamReader(channel.getInvertedOut()))) {

            final CharBuffer buffer = CharBuffer.allocate(100 * 1024);
            channel.open().verify(timeout.toMillis());

            while (channel.isOpen()) {
                //noinspection ResultOfMethodCallIgnored
                readFromHost.read(buffer);
            }

            buffer.flip(); // prepare reading
            final Integer exitStatus = channel.getExitStatus();
            return CommandResult.of(exitStatus, buffer);
        }
    }

    private void copySshIdentity(final ClientSession session) throws IOException, InvalidKeySpecException {

        final String sshIdentity = keyPairFileStorage.getPublicKeyAsSshIdentityString();
        final int userPart = sshIdentity.lastIndexOf("mscv");
        final String sshPublicKey = sshIdentity.substring(0, userPart).trim();

        executeCommand(session, "mkdir -p ~/.ssh");
        final CommandResult grepStatus = executeCommand(session, "grep --color=never '" + sshPublicKey + "' " + AUTHORIZED_KEYS_FILE);

        if (grepStatus.exitCode.equals(0)) {
            logger.debug("Public key exists already. Nothing to do anymore");
        } else {
            executeCommand(session, "echo \"" + sshIdentity + "\" >>" + AUTHORIZED_KEYS_FILE);
            executeCommand(session, "chmod 600 " + AUTHORIZED_KEYS_FILE);
        }

        logger.info("Successfully logged into {} to exchange keys", session.getConnectAddress());
    }

    private static class CommandResult {
        private final Integer exitCode;
        private final CharBuffer output;

        public CommandResult(final Integer exitCode, final CharBuffer output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        public static CommandResult of(final Integer exitCode, final CharBuffer output) {
            return new CommandResult(exitCode, output);
        }
    }

    private class QueueRunner implements Callable<Queue<VerificationResultDetail>> {
        private final Logger logger = LogManager.getLogger();
        private final Queue<VerificationResultDetail> queue;
        private final Queue<VerificationResultDetail> results;
        private final ScpClient scpClient;
        private final OS targetOs;

        private QueueRunner(final Queue<VerificationResultDetail> queue, final ScpClient scpClient, final OS targetOs) {
            this.queue = queue;
            this.scpClient = scpClient;
            this.targetOs = targetOs;
            this.results = new ArrayDeque<>();
        }

        @Override
        public Queue<VerificationResultDetail> call() {
            try {
                VerificationResultDetail details;
                while ((details = queue.poll()) != null) {
                    results.offer(executeScripts(details, targetOs));
                }
            } catch (final Exception e) {
                logger.fatal("QueueRunner died from {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            }
            return results;
        }

        private VerificationResultDetail executeScripts(final VerificationResultDetail details, final OS targetOs) {
            try {
                final Script script = details.getScript();

                if (Objects.nonNull(script) && targetOs.equals(script.getOs())) {

                    final Path path = Path.of(script.getPhysicalPath());
                    final CommandResult scriptResult;

                    scpClient.upload(path, "/tmp" + path.getFileName(), ScpClient.Option.PreserveAttributes);
                    executeCommand(scpClient.getSession(), "chmod u+x /tmp" + path.getFileName());

                    scriptResult = executeCommand(scpClient.getSession(), "/tmp" + path.getFileName());

                    if (scriptResult.exitCode.equals(0)) {
                        details.setResult(DetailSuccessIndicator.SUCCESS);
                    } else {
                        details.setResult(DetailSuccessIndicator.ERROR);
                    }

                    details.setDetails(scriptResult.output.toString());
                } else {
                    details.setResult(DetailSuccessIndicator.NOT_APPLICABLE);
                    details.setDetails("No script applicable for " + targetOs);
                }

            } catch (final Exception e) {
                details.setResult(DetailSuccessIndicator.FAILURE);
                details.setDetails(e.getMessage());
            }
            return details;
        }
    }
}
