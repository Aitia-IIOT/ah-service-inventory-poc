package eu.arrowhead.common.dto.shared.mscv;


import java.io.Serializable;
import java.util.StringJoiner;

public class MipDto implements Serializable {

    private Integer extId;
    private String name;
    private String description;
    private StandardDto standard;
    private CategoryDto category;
    private DomainDto domain;

    public MipDto() {
        super();
    }

    public MipDto(final Integer extId, final String name, final StandardDto standard, final CategoryDto category, final DomainDto domain) {
        this.extId = extId;
        this.name = name;
        this.standard = standard;
        this.category = category;
        this.domain = domain;
    }

    public Integer getExtId() {
        return extId;
    }

    public void setExtId(final Integer extId) {
        this.extId = extId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public StandardDto getStandard() {
        return standard;
    }

    public void setStandard(final StandardDto standard) {
        this.standard = standard;
    }

    public CategoryDto getCategory() {
        return category;
    }

    public void setCategory(final CategoryDto category) {
        this.category = category;
    }

    public DomainDto getDomain() {
        return domain;
    }

    public void setDomain(final DomainDto domain) {
        this.domain = domain;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MipDto.class.getSimpleName() + "[", "]")
                .add("extId=" + extId)
                .add("name='" + name + "'")
                .add("description='" + description + "'")
                .add("standard=" + standard)
                .add("category=" + category)
                .add("domain=" + domain)
                .toString();
    }
}
