package de.smartsquare.starter.mqtt

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import kotlin.reflect.KClass

/**
 * Annotation to mark properties or fields to be validated as a mqtt version.
 */
@Constraint(validatedBy = [MqttVersionValidator::class])
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class MqttVersion(
    val message: String = "Invalid mqtt version. Allowed are 3 and 5.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<*>> = [],
)

/**
 * Custom validator for the mqtt version, enabled by the [MqttVersion] annotation.
 */
class MqttVersionValidator : ConstraintValidator<MqttVersion, Int> {

    override fun isValid(value: Int?, context: ConstraintValidatorContext): Boolean {
        return value == null || value == 3 || value == 5
    }
}
