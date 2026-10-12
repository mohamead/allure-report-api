package no.spk.allure.report

/** Thrown when a report cannot be generated because of invalid input. */
class AllureReportException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
