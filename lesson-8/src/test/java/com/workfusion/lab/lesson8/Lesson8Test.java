/*
 * Copyright (C) WorkFusion 2018. All rights reserved.
 */
package com.workfusion.lab.lesson8;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.workfusion.lab.lesson8.config.Assignment1ModelConfiguration;
import com.workfusion.lab.lesson8.config.Assignment2ModelConfiguration;
import com.workfusion.lab.lesson8.fe.IsNerPresentFE;
import com.workfusion.lab.lesson8.fe.KeywordsPreviousLineFE;
import com.workfusion.lab.lesson8.fe.NerAbsolutePositionFE;
import com.workfusion.lab.lesson8.fe.SimilarityKeysInPrevLineFE;
import com.workfusion.lab.lesson8.processing.TotalAmountPostProcessor;
import com.workfusion.lab.lesson8.run.Assignment1ModelExecutionRunner;
import com.workfusion.lab.lesson8.run.Assignment1ModelTrainingRunner;
import com.workfusion.lab.lesson8.run.Assignment2ModelExecutionRunner;
import com.workfusion.lab.lesson8.run.Assignment2ModelTrainingRunner;
import com.workfusion.lab.model.TestTokenFeatures;
import com.workfusion.lab.utils.BaseLessonTest;
import com.workfusion.vds.nlp.model.configuration.ConfigurationData;
import com.workfusion.vds.sdk.api.nlp.annotator.Annotator;
import com.workfusion.vds.sdk.api.nlp.configuration.FieldInfo;
import com.workfusion.vds.sdk.api.nlp.configuration.FieldType;
import com.workfusion.vds.sdk.api.nlp.model.Field;
import com.workfusion.vds.sdk.api.nlp.model.IeDocument;
import com.workfusion.vds.sdk.api.nlp.model.NamedEntity;
import com.workfusion.vds.sdk.api.nlp.model.Token;

public class Lesson8Test extends BaseLessonTest {
    /**
     * Assignment 1:
     * <p>
     * Provide a model configuration for field "total_amount". You need to specify the @Named("annotators") method results to have:
     * <p>
     * Annotators:
     *   - a token provider annotator, that injects Token elements into document.
     *   Use SplitterTokenAnnotator() annotator to return an implementation instance. Regexp: ([$\$\s:#_;']).
     *   - am annotator to create EntityBoundary elements in document.
     *   Use EntityBoundaryAnnotator() to return an implementation instance.
     *   - a NER provider annotator. Use getJavaPatternRegexNerAnnotator annotator to create NERs in document with
     *   mentionType 'total_amount' and regex '[0-9l]{1,3}[,\.]?[0-9]{2,3}[\.][0-9]*'.
     *
     * You need to specify the @Named("featureExtractors") method results to have:
     * Features Extractors:
     *   - provide a custom FE NerAbsolutePositionFE() that adds a feature (with name="lastnerFeature") for elements, which is covered by a
     *     NamedEntity and the last NER in the document.
     *   - provide a custom FE KeywordsPreviousLineFE() that adds a feature (with name="keywordFeature") and value is 1 for
     *     element if provided keyword (in this case key is 'total') was found in the previous line (new Feature(FEATURE_NAME, 1)));
     * Custom Post-Processors for the field:
     *   - PricePostProcessor: a post-processor (use the provided TotalAmountPostProcessor.java template) that converts the "total_amount" field:
     *     from "$7,858.62" -> "7858.62 USD" formats. Check the provided gold documents from "data/test/*.html" for details.
     */
    @Test
    public void assignment1() throws Exception {
        // Creates ML-SDK Document to process
        IeDocument document = getDocument("documents/lesson_8_assignment_1.html");

        // Obtains model configuration for field "total_amount"
        ConfigurationData configurationData = buildConfiguration(Assignment1ModelConfiguration.class,
                new FieldInfo.Builder("total_amount").type(FieldType.TEXT).build());

        // Obtains defined annotators list for field "total_amount".
        List<Annotator> annotators = getAnnotatorsFromConfiguration(configurationData, 3);

        // Process annotators for field "total_amount"
        processAnnotators(document, annotators);

        // Gets all Tokens provided by the annotator to check for field "total_amount"
        List<Token> tokens = new ArrayList<>(document.findAll(Token.class));
        // Checks the provided token with the assignment 3 pattern for field "total_amount"
        checkElements(tokens, "lesson_8_assignment_1_check_token.json");

        // Checks the provided ners with the assignment 3 pattern for field "total_amount"
        List<NamedEntity> ners = new ArrayList<>(document.findAll(NamedEntity.class));
        checkElements(ners, "lesson_8_assignment_1_check_ners.json");

        // Process FEs list
        List<TestTokenFeatures> providedElementFeatures = processFeatures(document,
                new NerAbsolutePositionFE(), new KeywordsPreviousLineFE("total") //Assignment FE to check
        );
        // Checks the provided Features with the assignment 3 pattern
        checkElementFeatures(providedElementFeatures, "lesson_8_assignment_1_check_fe.json");

        // Adds Fields into document based on gold tagging
        addFields(document, Assignment1ModelConfiguration.FIELD_TOTAL_AMOUNT);

        // Process postprocessor
        processPostProcessor(document, new TotalAmountPostProcessor()); //Assignment post-processor

        // Gets all Fields provided by the Processor to check
        List<Field> fields = new ArrayList<>(document.findFields(Assignment1ModelConfiguration.FIELD_TOTAL_AMOUNT));

        // Checks the provided fields with the assignment's pattern
        checkElements(fields, "lesson_8_assignment_1_check_pp.json");

        // Obtains training statistics
        executeRunner(Assignment1ModelTrainingRunner.class);
        Map<String, FieldStatistic> trainingStatistics = getTrainingFieldStatistics(Assignment1ModelTrainingRunner.OUTPUT_DIR_PATH);

        // Check the field statistics
        checkFieldStatistics(trainingStatistics, Assignment1ModelConfiguration.FIELD_TOTAL_AMOUNT, 0.9, 0.6);

        executeRunner(Assignment1ModelExecutionRunner.class);
        Map<String, FieldStatistic> executionStatistics = getExecutionFieldStatistics(Assignment1ModelTrainingRunner.OUTPUT_DIR_PATH + "/extract");

        // Check the field statistics
        checkFieldStatistics(executionStatistics, Assignment1ModelConfiguration.FIELD_TOTAL_AMOUNT, 0.9, 0.6);
    }

    /**
     * Assignment 2:
     * <p>
     * Provide a model configuration for field "client_address_state". You need to specify the @Named("annotators") method results to have:
     * <p>
     * Annotators:
     *   - a token provider annotator, that injects Token elements into document.
     *   Use SplitterTokenAnnotator() annotator to return an implementation instance. Regexp: ([$\$\s:#_;']).
     *   - am annotator to create EntityBoundary elements in document.
     *   Use EntityBoundaryAnnotator() to return an implementation instance.
     *   - a NER provider annotator. Use getJavaPatternRegexNerAnnotator annotator to create NERs in document with
     *   type 'state' and regex '[A-Z]{2}'.
     *
     * Features Extractors:
     *   - provide a custom FE IsNerPresentFE() that adds a feature (with name="nerFeature") for elements, which is covered by a
     *     NamedEntity with type="state".
     *   - provide a custom FE SimilarityKeysInPrevLineFE() that adds a feature (with name="cosSimilarity") and value is highest cosine similarity of
     *     element to provided keyword (in this case key is 'address') in the previous line. You need to use cosine word similarity algorithms to gets similarity of element to provided keyword
     *     in the previous line. (new Feature(FEATURE_NAME, bestCosineScore)));
     */
    @Test
    public void assignment2() throws Exception {
        // Creates ML-SDK Document to process
        IeDocument document = getDocument("documents/lesson_8_assignment_1.html");

        // Obtains model configuration for field "client_address_state"
        ConfigurationData configurationData = buildConfiguration(Assignment2ModelConfiguration.class,
                new FieldInfo.Builder("client_address_state").type(FieldType.TEXT).build());

        // Obtains defined annotators list for field "client_address_state".
        List<Annotator> annotators = getAnnotatorsFromConfiguration(configurationData, 3);

        // Process annotators for field "state"
        processAnnotators(document, annotators);

        // Gets all Tokens provided by the annotator to check for field "client_address_state"
        List<Token> tokens = new ArrayList<>(document.findAll(Token.class));
        // Checks the provided token with the assignment 2 pattern for field "client_address_state"
        checkElements(tokens, "lesson_8_assignment_2_check_token.json");

        // Checks the provided ners with the assignment 2 pattern for field "client_address_state"
        List<NamedEntity> ners = new ArrayList<>(document.findAll(NamedEntity.class));
        checkElements(ners, "lesson_8_assignment_2_check_ners.json");

        // Process FEs list
        List<TestTokenFeatures> providedElementFeatures = processFeatures(document,
                new IsNerPresentFE("state"), new SimilarityKeysInPrevLineFE("address") //Assignment FE to check
        );

        // Checks the provided Features with the assignment 2 pattern
        checkElementFeatures(providedElementFeatures, "lesson_8_assignment_2_check_fe.json");

        // Obtains training statistics
        executeRunner(Assignment2ModelTrainingRunner.class);
        Map<String, FieldStatistic> trainingStatistics = getTrainingFieldStatistics(Assignment2ModelTrainingRunner.OUTPUT_DIR_PATH);

        // Check the field statistics
        checkFieldStatistics(trainingStatistics, Assignment2ModelConfiguration.FIELD_STATE, 0.9, 0.6);

        executeRunner(Assignment2ModelExecutionRunner.class);
        Map<String, FieldStatistic> executionStatistics = getExecutionFieldStatistics(Assignment2ModelTrainingRunner.OUTPUT_DIR_PATH + "/extract");

        // Check the field statistics
        checkFieldStatistics(executionStatistics, Assignment2ModelConfiguration.FIELD_STATE, 0.9, 0.6);
    }

}