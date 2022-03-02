/*
 * Copyright (C) WorkFusion 2018. All rights reserved.
 */
package com.workfusion.lab.lesson6;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.workfusion.lab.lesson6.config.Assignment1ModelConfiguration;
import com.workfusion.lab.lesson6.config.Assignment2ModelConfiguration;
import com.workfusion.lab.utils.BaseLessonTest;
import com.workfusion.vds.nlp.model.configuration.ConfigurationData;
import com.workfusion.vds.sdk.api.nlp.annotator.Annotator;
import com.workfusion.vds.sdk.api.nlp.configuration.FieldInfo;
import com.workfusion.vds.sdk.api.nlp.configuration.FieldType;
import com.workfusion.vds.sdk.api.nlp.fe.FeatureExtractor;
import com.workfusion.vds.sdk.api.nlp.model.EntityBoundary;
import com.workfusion.vds.sdk.api.nlp.model.IeDocument;
import com.workfusion.vds.sdk.api.nlp.model.NamedEntity;
import com.workfusion.vds.sdk.api.nlp.model.Sentence;
import com.workfusion.vds.sdk.api.nlp.model.Token;

import static org.assertj.core.api.Assertions.assertThat;

public class Lesson6Test extends BaseLessonTest {

    /**
     * Assignment 1:
     * <p>
     * Provide a model configuration for two fields("invoice_number" and "client_address"). You need to specify the @Named("annotators") method results to have:
     * <p>
     * Annotators:
     * - for both fields add:
     *   1. a token provider annotator, that injects Token elements into document.
     *   Use MatcherTokenAnnotator() annotator to return an implementation instance. Regexp: \W+.
     *   2. - Annotator to create {@link EntityBoundary} elements in document.
     *   Use EntityBoundaryAnnotator() to return an implementation instance.
     *
     * - for field "invoice_number" add:
     * - a NER provider annotator. Use getJavaPatternRegexNerAnnotator annotator to create NERs in document with
     *   mentionType 'invoice number' and regex '[0-9]{11}'.

     * - for field "client_address" add:
     * - a NER provider annotator. Use AhoCorasickDictionaryNerAnnotator annotator, that injects Token elements into document
     *   with mentionType 'country' and dictionary "classpath:dictionary/countries.csv".
     */
    protected List<Annotator> annotators = new ArrayList<>();

    @Test
    public void assignment1() throws Exception {
        // Creates ML-SDK Document to process
        IeDocument document = getDocument("documents/lesson_6_assignment_1.txt");

        // Obtains model configuration for field "invoice_number"
        ConfigurationData configurationData = buildConfiguration(Assignment1ModelConfiguration.class,
            new FieldInfo.Builder("invoice_number").type(FieldType.TEXT).build());

        // Obtains defined annotators list for field "invoice_number".
        List<Annotator> annotators = getAnnotatorsFromConfiguration(configurationData, 3);

        // Process annotators for field "invoice_number"
        processAnnotators(document, annotators);

        // Gets all Tokens provided by the annotator to check for field "invoice_number"
        List<Token> tokens = new ArrayList<>(document.findAll(Token.class));

        // Checks the provided token with the assignment 1 pattern for field "invoice_number"
        checkElements(tokens, "lesson_6_assignment_1_check_token_1.json");

        // Checks the provided ners with the assignment 1 pattern for field "invoice_number"

        List<NamedEntity> ners = new ArrayList<>(document.findAll(NamedEntity.class));
        checkElements(ners, "lesson_6_assignment_1_check_ners_1_in.json");

        // Gets all Sentence provided by the annotator to check
        List<Sentence> sentences = new ArrayList<>(document.findAll(Sentence.class));
        // Checks the provided sentences with the assignment 1 pattern
        checkElements(sentences, "lesson_6_assignment_1_check_sentences_1.json");

        configurationData = buildConfiguration(Assignment1ModelConfiguration.class,
                new FieldInfo.Builder("client_address").type(FieldType.TEXT).build());

        // Obtains defined annotators list for field "client_address".
        annotators = getAnnotatorsFromConfiguration(configurationData, 3);

        // Process annotators for field "client_address"
        processAnnotators(document, annotators);

        // Gets all Tokens provided by the annotator to check for field "client_address"
        tokens = new ArrayList<>(document.findAll(Token.class));

        // Checks the provided token with the assignment 1 pattern for field "client_address"
        checkElements(tokens, "lesson_6_assignment_1_check_token_2.json");

        // Checks the provided ners with the assignment 1 pattern for field "client_address"
        ners = new ArrayList<>(document.findAll(NamedEntity.class));
        checkElements(ners, "lesson_6_assignment_1_check_ners_2_ca.json");
    }

    /**
     * Assignment 2:
     * <p>
     * Provide a model configuration for two type of field("FREE_TEXT" and "INVOICE_TYPE"). You need to specify the @Named("featureExtractors") method results to have:
     * <p>
     * FE:
     * - for field type "FREE_TEXT" add IsAllLettersUpperCase()
     * - for field type "INVOICE_TYPE" add IsOnlyNumberInTokenFE()
     */
    protected List<FeatureExtractor> fes = new ArrayList<>();

    @Test
    public void assignment2() throws Exception {

        // Variables definitions
        ConfigurationData configurationData;
        List<FeatureExtractor> fes;

        // Obtains model configuration
        configurationData = buildConfiguration(Assignment2ModelConfiguration.class,
                new FieldInfo.Builder("address").type(FieldType.TEXT).build());

        // Obtains defined annotators list.
        fes = getFEsFromConfiguration(configurationData, 1);
        log("Checking that the IsAllLettersUpperCase() is present for type 'FREE_TEXT'.");
        assertThat(fes.get(0).getClass()).isEqualTo(Assignment2ModelConfiguration.IsAllLettersUpperCase.class);

        // Checking invoice_number
        // Obtains model configuration
        configurationData = buildConfiguration(Assignment2ModelConfiguration.class,
                new FieldInfo.Builder("invoice_number").type(FieldType.NUMBER).build());

        // Obtains defined fe list.
        fes = getFEsFromConfiguration(configurationData, 1);
        log("Checking that the IsOnlyNumberInTokenFE() is present for type 'INVOICE_NUMBER'.");
        assertThat(fes.get(0).getClass()).isEqualTo(Assignment2ModelConfiguration.IsOnlyNumberInTokenFE.class);
    }

}