package io.kontur.eventapi.resource.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintValidatorContext;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class BboxValidatorTest {

    private BboxValidator validator;
    private ConstraintValidatorContext ctx;
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;

    @BeforeEach
    public void setUp() {
        validator = new BboxValidator();
        ctx = mock(ConstraintValidatorContext.class);
        builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(ctx.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addConstraintViolation()).thenReturn(ctx);
    }

    @Test
    public void testValidBbox() {
        List<BigDecimal> bbox = List.of(
                BigDecimal.valueOf(10), BigDecimal.valueOf(-10),
                BigDecimal.valueOf(20), BigDecimal.valueOf(0)
        );
        assertTrue(validator.isValid(bbox, ctx));
    }

    @Test
    public void testInvalidSize() {
        List<BigDecimal> bbox = List.of(BigDecimal.ONE, BigDecimal.ONE);
        assertFalse(validator.isValid(bbox, ctx));
    }

    @Test
    public void testOutOfRange() {
        List<BigDecimal> bbox = List.of(
                BigDecimal.valueOf(181), BigDecimal.ZERO,
                BigDecimal.valueOf(182), BigDecimal.ONE
        );
        assertFalse(validator.isValid(bbox, ctx));
    }

    @Test
    public void testReversedCoordinates() {
        List<BigDecimal> bbox = List.of(
                BigDecimal.valueOf(20), BigDecimal.valueOf(10),
                BigDecimal.valueOf(10), BigDecimal.valueOf(20)
        );
        assertFalse(validator.isValid(bbox, ctx));
    }
}
