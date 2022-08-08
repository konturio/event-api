package io.kontur.eventapi.resource.dto;

import io.kontur.eventapi.resource.validation.ValidBbox;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.math.BigDecimal;
import java.util.List;

public class BboxValidator implements ConstraintValidator<ValidBbox, List<BigDecimal>> {
    @Override
    public void initialize(ValidBbox value) {
    }

    @Override
    public boolean isValid(List<BigDecimal> bbox, ConstraintValidatorContext ctx) {
        if (bbox == null || bbox.isEmpty()) {
            return true;
        }
        if (bbox.size() != 4) {
            ctx.buildConstraintViolationWithTemplate("bbox should be provided as 4 numbers.")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }

    private boolean checkLat(BigDecimal lat) {
        return BigDecimal.valueOf(90).compareTo(lat) >= 0 && BigDecimal.valueOf(-90).compareTo(lat) <= 0;
    }

    private boolean checkLon(BigDecimal lon) {
        return BigDecimal.valueOf(180).compareTo(lon) >= 0 && BigDecimal.valueOf(-180).compareTo(lon) <= 0;
    }


}

