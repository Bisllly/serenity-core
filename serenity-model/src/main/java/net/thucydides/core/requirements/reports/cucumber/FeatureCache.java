package net.thucydides.core.requirements.reports.cucumber;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.requirements.model.cucumber.AnnotatedFeature;
import net.thucydides.core.requirements.model.cucumber.CucumberParser;
import net.thucydides.core.util.EnvironmentVariables;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static net.thucydides.core.ThucydidesSystemProperty.SERENITY_REQUIREMENTS_CACHE_HEAP_SIZE;

public class FeatureCache {

    private static final FeatureCache FEATURE_CACHE = new FeatureCache();
    private final CucumberParser parser;

    LoadingCache<String, Optional> cache;

    public static FeatureCache getCache() {
        return FEATURE_CACHE;
    }

    protected FeatureCache() {
        EnvironmentVariables environmentVariables = Injectors.getInjector().getInstance(EnvironmentVariables.class);
        int maxHeap = SERENITY_REQUIREMENTS_CACHE_HEAP_SIZE.integerFrom(environmentVariables, 1024);

        cache = Caffeine.newBuilder()
                .maximumSize(maxHeap)
                .build(this::loadFeatureFileFrom);

        this.parser = new CucumberParser();
    }

    public Optional<AnnotatedFeature> loadFeature(File featureFile) {
        return loadFeature(featureFile.getPath());
    }

    public Optional<AnnotatedFeature> loadFeature(String featureFilePath) {
//        return parser.loadFeature(new File(featureFilePath));
        return cache.get(featureFilePath);
    }

    private Optional<AnnotatedFeature> loadFeatureFileFrom(String featureFilePath) {
        return parser.loadFeature(new File(featureFilePath));
    }

    // Do not forget to close the CacheManager when your application ends
    public void close() {
        cache.invalidateAll();
    }
}
