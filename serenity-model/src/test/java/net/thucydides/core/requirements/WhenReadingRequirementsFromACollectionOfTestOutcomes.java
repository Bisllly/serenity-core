package net.thucydides.core.requirements;

import net.thucydides.core.environment.MockEnvironmentVariables;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestTag;
import net.thucydides.core.reports.TestOutcomeLoader;
import net.thucydides.core.requirements.model.Requirement;
import net.thucydides.core.util.EnvironmentVariables;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The goal here is to load a requirements hierarchy from an arbitrary collection of test outcomes.
 * This can include test outcomes from JUnit 4 or 5 tests, or from Serenity/JS tests.
 * The goal is to reduce the reliance on reading from the classpath, which has proven to be unreliable.
 */
class WhenReadingRequirementsFromACollectionOfTestOutcomes {

    EnvironmentVariables environmentVariables = new MockEnvironmentVariables();

    @Nested
    class WhenTheTestOutcomesAreFromJUnit4Tests {

        @Test
        void testOutcomesShouldBeReadFromTheTargetDirectory() {
            environmentVariables.setProperty("serenity.outputDirectory", localResource("sample-junit4-outcomes"));
            environmentVariables.setProperty("serenity.test.root", "smoketests");
            TestOutcomeRequirementsTagProvider tagProvider = new TestOutcomeRequirementsTagProvider(environmentVariables);
            assertThat(tagProvider.getRequirements()).isNotEmpty();
        }

        // Read a test outcome from a JUnit 4 test with a single package level
        @Test
        void shouldReadATestOutcomeFromAJUnit4TestWithASinglePackageLevel() {
            environmentVariables.setProperty("serenity.outputDirectory", localResource("sample-junit4-outcomes"));
            environmentVariables.setProperty("serenity.test.root", "smoketests.sample");
            TestOutcomeRequirementsTagProvider tagProvider = new TestOutcomeRequirementsTagProvider(environmentVariables);
            assertThat(tagProvider.getRequirements()).isNotEmpty();
            assertThat(tagProvider.getRequirements()).hasSize(2);
            assertThat(tagProvider.getRequirements().get(0).hasChildren()).isTrue();
            assertThat(tagProvider.getRequirements().get(0).getChildren()).allMatch(
                    child -> child.getParent().equals("nested")
            );

        }

        @Test
        void shouldFindTheParentOfARequirement() {
            environmentVariables.setProperty("serenity.outputDirectory", localResource("sample-junit4-outcomes"));
            environmentVariables.setProperty("serenity.test.root", "smoketests.sample");
            TestOutcomeRequirementsTagProvider tagProvider = new TestOutcomeRequirementsTagProvider(environmentVariables);

            Requirement parent = tagProvider.getRequirements().get(0);
            Requirement child = parent.getChildren().get(0);
            assertThat(tagProvider.getParentRequirementOf(child)).isPresent().get().isEqualTo(parent);
        }

        // - Read a test outcome from a JUnit 4 test with nested packages
        @Test
        void shouldReadATestOutcomeFromAJUnit4TestWithNestedPackages() {
            environmentVariables.setProperty("serenity.outputDirectory", localResource("sample-junit4-outcomes"));
            environmentVariables.setProperty("serenity.test.root", "smoketests");
            TestOutcomeRequirementsTagProvider tagProvider = new TestOutcomeRequirementsTagProvider(environmentVariables);
            assertThat(tagProvider.getRequirements()).isNotEmpty();
            assertThat(tagProvider.getRequirements()).hasSize(1);
            assertThat(tagProvider.getRequirements().get(0).hasChildren()).isTrue();
            assertThat(tagProvider.getRequirements().get(0).getChildren()).allMatch(
                    child -> child.getParent().equals("sample"));
            assertThat(tagProvider.getRequirements().get(0).getChildren().get(0).getChildren()).allMatch(
                    child -> child.getParent().equals("sample/nested"));
        }

        // Can list the requirement types based on the depth of the requirements hierarchy
        @Test
        void shouldListTheRequirementTypesBasedOnTheDepthOfTheRequirementsHierarchy() {
            environmentVariables.setProperty("serenity.outputDirectory", localResource("sample-junit4-outcomes"));
            environmentVariables.setProperty("serenity.test.root", "smoketests");
            TestOutcomeRequirementsTagProvider tagProvider = new TestOutcomeRequirementsTagProvider(environmentVariables);
            assertThat(tagProvider.getActiveRequirementTypes()).containsExactly("theme", "capability", "feature", "story");
        }

        @Test
        void shouldFindParentRequirementsForTestOutcomes() throws IOException {
            environmentVariables.setProperty("serenity.outputDirectory", localResource("sample-junit4-outcomes"));
            environmentVariables.setProperty("serenity.test.root", "smoketests");
            TestOutcomeRequirementsTagProvider tagProvider = new TestOutcomeRequirementsTagProvider(environmentVariables);

            List<? extends TestOutcome> testOutcomes
                    = TestOutcomeLoader.testOutcomesIn(new File(localResource("sample-junit4-outcomes"))).getOutcomes();

            Optional<Requirement> requirement = tagProvider.getParentRequirementOf(testOutcomes.get(0));

            assertThat(requirement).isPresent()
                    .matches(req -> req.get().getType().equals("story"));
        }

        @Test
        void shouldIdentifyTestTagsForATestOutcome() throws IOException {
            environmentVariables.setProperty("serenity.outputDirectory", localResource("sample-junit4-outcomes"));
            environmentVariables.setProperty("serenity.test.root", "smoketests");
            TestOutcomeRequirementsTagProvider tagProvider = new TestOutcomeRequirementsTagProvider(environmentVariables);

            List<? extends TestOutcome> testOutcomes
                    = TestOutcomeLoader.testOutcomesIn(new File(localResource("sample-junit4-outcomes"))).getOutcomes();

            TestOutcome outcome = testOutcomes.get(1);

            Set<TestTag> matchingTags = tagProvider.getTagsFor(outcome);

            assertThat(matchingTags).isNotEmpty();
        }
    }

    @Nested
    class WhenTheTestOutcomesAreFromCucumberTests {
        @Test
        void shouldIgnoreCucumberTestOutcomes() {
            environmentVariables.setProperty("serenity.outputDirectory", localResource("sample-cucumber-and-junit-outcomes"));
            environmentVariables.setProperty("serenity.test.root", "smoketests");
            TestOutcomeRequirementsTagProvider tagProvider = new TestOutcomeRequirementsTagProvider(environmentVariables);
            assertThat(tagProvider.getRequirements()).isNotEmpty();
        }
    }

    private String localResource(String directoryName) {
        return getClass().getClassLoader().getResource(directoryName).getPath();
    }

}
