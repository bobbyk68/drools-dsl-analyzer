package uk.gov.hmrc.cleaner.model;

public record ParsedDslEntry(String rawBlock, String plainTextToken, int lineNumber) {}
