# Drools DSL Analyzer

A modular, decoupled Java 17 utility designed to analyze Drools DSL (Domain Specific Language) mapping files against their corresponding DSLR (Rule) definitions. It detects redundant or obsolete `[then]` mapping entries, generates structural reporting, and creates optimized, cleaned output files.

This project is built using the **Strategy Design Pattern**, allowing you to seamlessly toggle between a custom regular-expression-based parsing engine and the official third-party `drools-drl-parser` syntax tree framework.

## Key Features

- **Decoupled Architecture:** No "God Classes". Follows clean SOLID engineering practices with independent components for discovery, parsing, validation, and reporting.
- **Strategy Pattern Integration:** Toggle dynamically between custom Regex and official Drools AST parsers.
- **Whitespace & Structural Normalization:** Handles multi-line DSL declarations spanning across CRLF boundaries, ignoring developer-specific spacing artifacts.
- **Safe Code Generation:** Does *not* alter your original workspace source files. Instead, it generates a side-by-side optimized file (`validationResult-BRXXX-cleaned.dsl`).
- **Dry-Run Capabilities:** Includes a conditional execution flag to perform end-to-end trace validation and print reports without performing downstream filesystem writes.
- **Enterprise Logging:** Standardized output routing utilizing an SLF4J facade with a Logback engine. Fully compliant with flat-file summary exports.

## Project Layout

```text
drools-dsl-analyzer
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── uk
    │   │       └── gov
    │   │           └── hmrc
    │   │               └── cleaner
    │   │                   ├── Application.java              # Bootstrapping Entry Point
    │   │                   ├── DslAnalyzerEngine.java        # Process Coordinator Context
    │   │                   ├── DslParsingStrategy.java       # Unified Extraction Interface
    │   │                   ├── RegexDslParsingStrategy.java  # Custom Heavy-Regex Parser
    │   │                   ├── DroolsDslParsingStrategy.java # Third-Party AST Library Parser
    │   │                   ├── DslrAnalyzer.java             # DSLR Keyword Token Evaluator
    │   │                   ├── ReportGenerator.java          # Aggregator and File Logger
    │   │                   └── RuleSetLocator.java           # Workspace Directory Scanner
    │   └── resources
    │       └── logback.xml                                   # Console Pattern Configurations
    └── test
        └── java
            └── uk
                └── gov
                    └── hmrc
                        └── cleaner
                            └── DslParserTest.java            # Automated Boundary Assertion
