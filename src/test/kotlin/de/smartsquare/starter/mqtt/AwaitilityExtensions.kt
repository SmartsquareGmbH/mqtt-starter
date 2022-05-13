package de.smartsquare.starter.mqtt

import org.amshove.kluent.internal.ComparisonFailedException
import org.awaitility.core.ConditionFactory

// Adjusted version of Awaitility's untilAsserted for kluent. Kluent does not throw a subclass of AssertionError as
// Awaitility expects but instead the ComparisonFailedException, which this function maps to the former.
infix fun ConditionFactory.untilAssertedKluent(fn: () -> Unit): Unit = untilAsserted {
    try {
        fn()
    } catch (error: ComparisonFailedException) {
        throw AssertionError(error.message, error)
    }
}
