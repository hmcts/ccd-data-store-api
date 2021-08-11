package uk.gov.hmcts.ccd.domain.model.std.validator;

import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.domain.model.std.Parties;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

public class PartiesValidator implements ConstraintValidator<ValidPartiesItems, List<Parties>> {
    private static final Logger LOG = LoggerFactory.getLogger(PartiesValidator.class);


    @Override
    public boolean isValid(final List<Parties> partiesList, final ConstraintValidatorContext context) {
        if (partiesList != null) {
            Set<Field> sets = ReflectionUtils.getFields(Parties.class);
            for (Parties parties : partiesList) {
                int partyFields = 0;
                try {
                    for (Field field : sets) {
                        if (field.get(parties) != null) {
                            partyFields++;
                        }
                    }
                } catch (Exception e) {
                    LOG.error(e.getMessage());
                }
                if (partyFields < 2) {
                    return false;
                }
            }
        }
        return true;
    }
}
