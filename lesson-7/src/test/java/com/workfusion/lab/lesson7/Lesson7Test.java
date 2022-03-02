/*
 * Copyright (C) WorkFusion 2018. All rights reserved.
 */
package com.workfusion.lab.lesson7;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.workfusion.lab.lesson7.config.Assignment1ModelConfiguration;
import com.workfusion.lab.lesson7.config.Assignment2ModelConfiguration;
import com.workfusion.lab.lesson7.config.Assignment3ModelConfiguration;
import com.workfusion.lab.lesson7.config.Assignment4ModelConfiguration;
import com.workfusion.lab.lesson7.fe.Assignment1CoveringByLineFE;
import com.workfusion.lab.lesson7.fe.Assignment2FE;
import com.workfusion.lab.lesson7.fe.Assignment3IsCoveredByNerFE;
import com.workfusion.lab.lesson7.fe.Assignment4ColumnIndexFE;
import com.workfusion.lab.lesson7.fe.Assignment4NextTokenIsNumberFE;
import com.workfusion.lab.model.TestTokenFeatures;
import com.workfusion.lab.utils.BaseLessonTest;
import com.workfusion.vds.nlp.model.configuration.ConfigurationData;
import com.workfusion.vds.sdk.api.nlp.annotator.Annotator;
import com.workfusion.vds.sdk.api.nlp.configuration.FieldInfo;
import com.workfusion.vds.sdk.api.nlp.configuration.FieldType;
import com.workfusion.vds.sdk.api.nlp.fe.FeatureExtractor;
import com.workfusion.vds.sdk.api.nlp.model.Document;
import com.workfusion.vds.sdk.api.nlp.model.Token;
import com.workfusion.vds.sdk.nlp.component.annotator.EntityBoundaryAnnotator;
import com.workfusion.vds.sdk.nlp.component.annotator.tokenizer.MatcherTokenAnnotator;

import static org.assertj.core.api.Assertions.assertThat;

public class Lesson7Test extends BaseLessonTest {

    /**
     * Assignment 1:
     * Use the existing Assignment1ModelConfiguration.java template to define annotator+FE configuration that provides:
     * - a Token elements, Let's consider token as a word that contains on one or more word character, regexp:  \w+ or [a-zA-Z0-9_]+
     * - a "covering_by_line" feature for all token covered by Line element
     * Check the provided "documents/lesson_7_assignment_1.html" for details.
     * You must use:
     * - OOTB annotators
     * - the provided Assignment1CoveringByLineFE.java FE's template
     */
    @Test
    public void assignment1() throws Exception {
        // Creates ML-SDK Document to process
        Document document = getDocument("documents/lesson_7_assignment_1.html");

        // Obtains model configuration
        ConfigurationData configurationData = buildConfiguration(Assignment1ModelConfiguration.class);

        // Obtains defined annotators list.
        List<Annotator> annotators = getAnnotatorsFromConfiguration(configurationData, 1);

        // Obtains defined FEs list.
        List<FeatureExtractor> fes = getFEsFromConfiguration(configurationData, 1);
        // Checking provided annotators
        checkFeInList(fes, Assignment1CoveringByLineFE.class);

        // Process annotators
        processAnnotators(document, annotators);

        // Gets all Tokens provided by the annotator to check
        List<Token> tokens = new ArrayList<>(document.findAll(Token.class));
        // Checks the provided token with the assignment 1 pattern
        checkElements(tokens, "lesson_7_assignment_1a_check.json");

        // Process FEs list
        List<TestTokenFeatures> providedElementFeatures = processFeatures(document, fes);
        // Checks the provided Features with the assignment pattern
        checkElementFeatures(providedElementFeatures, "lesson_7_assignment_1b_check.json");
    }

    /**
     * Assignment 2:
     * Use the existing Assignment2ModelConfiguration.java template to define annotator+FE configuration that provides:
     * - a "invoice_number" feature for the token tagged by "invoice_number" fields
     * Check the provided "documents/lesson_7_assignment_2.html" for details.
     * You must use:
     * - the MatcherTokenAnnotator (use the provided Regexp) as a first annotator - to provide Token into document
     * - the provided Assignment2FE.java template for your solution
     * You are not allowed to work with element text in the FE, you should use OOTB annotator that provides NamedEntity and use them in the FE instead.
     */
    @Test
    public void assignment2() throws Exception {
        // Creates ML-SDK Document to process
        Document document = getDocument("documents/lesson_7_assignment_2.html");

        // Obtains model configuration for FIELD_INVOICE_NUMBER
        ConfigurationData configurationData = buildConfiguration(Assignment2ModelConfiguration.class,
                new FieldInfo.Builder(Assignment2ModelConfiguration.FIELD_INVOICE_NUMBER).type(FieldType.NUMBER).build());

        // Obtains defined annotators list.
        List<Annotator> annotators = getAnnotatorsFromConfiguration(configurationData);
        // Checking provided annotators
        assertThat(annotators.size()).isGreaterThanOrEqualTo(2);
        checkAnnotatorInList(annotators, MatcherTokenAnnotator.class, 0);
        checkAnnotatorInList(annotators, EntityBoundaryAnnotator.class, 1);

        // Obtains defined FEs list.
        List<FeatureExtractor> fes = getFEsFromConfiguration(configurationData, 1);
        // Checking provided annotators
        checkFeInList(fes, Assignment2FE.class);

        // Process annotators
        processAnnotators(document, annotators);
        checkTokenAreProvided(document);

        // Process FEs list
        List<TestTokenFeatures> providedElementFeatures = processFeatures(document, document, fes, false);
        // Checks the provided Features with the assignment pattern
       // writeTestElementFE("lesson_7_assignment_2_check.json");
        checkElementFeatures(providedElementFeatures, "lesson_7_assignment_2_check.json");
    }

    /**
     * Assignment 3:
     * Use the previous assignment results to change the existing Assignment3ModelConfiguration.java template. It should define annotator+FE
     * configuration for the following fields:
     * - invoice_number
     * - email
     * The configuration must be the fields content depended (revise the Lesson 5)
     * For the "invoice_number" content the configuration must provides:
     * - a "invoice_number" feature for the token tagged by "invoice_number" fields
     * For the "email" content the configuration must provides:
     * - a "email" feature for the token tagged by "email" fields
     * Check the provided "documents/lesson_7_assignment_3.html" for details.
     * You must use:
     * - the MatcherTokenAnnotator (use the provided Regexp) as a first annotator - to provide Token into document
     * - the provided Assignment3IsCoveredByNerFE.java template for your solution
     * You are not allowed to work with element text in the FE.
     */
    @Test
    public void assignment3() throws Exception {
        // FIELD_INVOICE_NUMBER check

        // Creates ML-SDK Document to process
        Document document = getDocument("documents/lesson_7_assignment_3.html");

        // Obtains model configuration for FIELD_INVOICE_NUMBER
        ConfigurationData configurationData = buildConfiguration(Assignment3ModelConfiguration.class,
                new FieldInfo.Builder(Assignment3ModelConfiguration.FIELD_INVOICE_NUMBER).type(FieldType.NUMBER).build());

        // Obtains defined annotators list.
        List<Annotator> annotators = getAnnotatorsFromConfiguration(configurationData);
        // Checking provided annotators
        assertThat(annotators.size()).isGreaterThanOrEqualTo(2);
        checkAnnotatorInList(annotators, MatcherTokenAnnotator.class, 0);
        checkAnnotatorInList(annotators, EntityBoundaryAnnotator.class, 1);

        // Obtains defined FEs list.
        List<FeatureExtractor> fes = getFEsFromConfiguration(configurationData, 1);
        // Checking provided annotators
        checkFeInList(fes, Assignment3IsCoveredByNerFE.class);

        // Process annotators
        processAnnotators(document, annotators);
        checkTokenAreProvided(document);

        // Process FEs list
        List<TestTokenFeatures> providedElementFeatures = processFeatures(document, document, fes, false);
        // Checks the provided Features with the assignment pattern
        checkElementFeatures(providedElementFeatures, "lesson_7_assignment_2_invoice_number_check.json");

        // FIELD_EMAIL check
        // Creates ML-SDK Document to process
        document = getDocument("documents/lesson_7_assignment_3.html");

        // Obtains model configuration for FIELD_EMAIL
        configurationData = buildConfiguration(Assignment3ModelConfiguration.class,
                new FieldInfo.Builder(Assignment3ModelConfiguration.FIELD_EMAIL).type(FieldType.EMAIL).build());

        // Obtains defined annotators list.
        annotators = getAnnotatorsFromConfiguration(configurationData, 3);
        // Checking provided annotators
        assertThat(annotators.size()).isGreaterThanOrEqualTo(2);
        checkAnnotatorInList(annotators, MatcherTokenAnnotator.class, 0);
        checkAnnotatorInList(annotators, EntityBoundaryAnnotator.class, 1);

        // Obtains defined FEs list.
        fes = getFEsFromConfiguration(configurationData, 1);
        // Checking provided annotators
        checkFeInList(fes, Assignment3IsCoveredByNerFE.class);

        // Process annotators
        processAnnotators(document, annotators);
        checkTokenAreProvided(document);

        // Process FEs list
        providedElementFeatures = processFeatures(document, document, fes, false);
        // Checks the provided Features with the assignment pattern
        checkElementFeatures(providedElementFeatures, "lesson_7_assignment_2_email_check.json");
    }

    /**
     * Assignment 4:
     * Use the existing Assignment4ModelConfiguration.java template to define annotator+FE configuration for the "price" field
     * (the configuration must be the fields content depended) that provides:
     * - a "price" feature for the token tagged by "price" fields
     * - a "column_index"=<column index> for the covering row if any
     * - a "next_token_number"=<1> if previous token contains only number('1234 token' in this case for 'token' feature will be created)
     * Check the provided "documents/lesson_7_assignment_4.html" for details.
     * You must use:
     * - the MatcherTokenAnnotator (use the provided Regexp) as a first annotator - to provide Token into document
     * - the provided Assignment4ColumnIndexFE.java template
     * - the provided Assignment4NextTokenIsNumberFE.java template
     * You are not allowed to work with element text in the FE.
     * You can use the Assignment3IsCoveredByNerFE.java from previous assignment.
     */
    @Test
    public void assignment4() throws Exception {
        // Creates ML-SDK Document to process
        Document document = getDocument("documents/lesson_7_assignment_4.html");

        // Obtains model configuration for FIELD_INVOICE_NUMBER
        ConfigurationData configurationData = buildConfiguration(Assignment4ModelConfiguration.class,
                new FieldInfo.Builder(Assignment4ModelConfiguration.FIELD_PRICE).type(FieldType.TEXT).build());

        // Obtains defined annotators list.
        List<Annotator> annotators = getAnnotatorsFromConfiguration(configurationData);
        // Checking provided annotators
        assertThat(annotators.size()).isGreaterThanOrEqualTo(2);
        checkAnnotatorInList(annotators, MatcherTokenAnnotator.class, 0);
        checkAnnotatorInList(annotators, EntityBoundaryAnnotator.class, 1);

        // Obtains defined FEs list.
        List<FeatureExtractor> fes = getFEsFromConfiguration(configurationData);

        // Checking provided annotators
        checkFeInList(fes, Assignment4NextTokenIsNumberFE.class, Assignment4ColumnIndexFE.class);

        // Process annotators
        processAnnotators(document, annotators);
        checkTokenAreProvided(document);

        List<TestTokenFeatures> providedElementFeatures = processFeatures(document, document, fes, false);
        // Checks the provided Features with the assignment pattern
        checkElementFeatures(providedElementFeatures, "lesson_7_assignment_4_check.json");
    }

}
