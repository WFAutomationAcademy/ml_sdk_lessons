/*
 * Copyright (C) WorkFusion 2018. All rights reserved.
 */
package com.workfusion.lab.lesson4;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.validator.routines.checkdigit.IBANCheckDigit;
import org.junit.Test;

import com.workfusion.lab.lesson4.processing.Assignment1DatePostProcessor;
import com.workfusion.lab.lesson4.processing.Assignment2TotalPostProcessor;
import com.workfusion.lab.lesson4.processing.Assignment3IBANPostProcessor;
import com.workfusion.lab.lesson4.processing.Assignment4PricePostProcessor;
import com.workfusion.lab.lesson4.processing.Assignment5SimilarityPostProcessor;
import com.workfusion.lab.lesson4.processing.Assignment6ExpandPostProcessor;
import com.workfusion.lab.utils.BaseLessonTest;
import com.workfusion.vds.nlp.similarity.StringSimilarityUtils;
import com.workfusion.vds.sdk.api.nlp.model.Document;
import com.workfusion.vds.sdk.api.nlp.model.Element;
import com.workfusion.vds.sdk.api.nlp.model.Field;
import com.workfusion.vds.sdk.api.nlp.model.IeDocument;

public class Lesson4Test extends BaseLessonTest {

    /**
     * <p><b>Assignment 1</b></p>
     * <p>
     *     Provide a custom Post-Processor which will format all dates to the specified pattern: {@code MM/dd/yy}.
     * </p>
     * <p>Tips:</p>
     * <ul>
     *     <li>Use {@link IeDocument#findField(String fieldName)} to find all fields with name {@code date}.</li>
     *     <li>Use {@link Field#getText()} to get raw field text.</li>
     *     <li>Use {@link Field#setValue(String value)} to set new value from formatted text.</li>
     * </ul>
     *
     * As a template, use {@link Assignment1DatePostProcessor}.
     */
    @Test
    public void assignment1() throws Exception {
        // Creates ML-SDK Document to process
        IeDocument document = getDocument("documents/lesson_4_assignment_1.html");
        // Adds Fields into document based on gold tagging
        addFields(document, Assignment1DatePostProcessor.FIELD_NAME);

        // Process postprocessor
        processPostProcessor(document,
                new Assignment1DatePostProcessor() //Assignment post-processor
        );

        // Gets all Fields provided by the Processor to check
        List<Field> fields = new ArrayList<>(document.findFields(Assignment1DatePostProcessor.FIELD_NAME));

        // Checks the provided fields with the assignment's pattern
        checkElements(fields, "lesson_4_assignment_1_check.json");
    }

    /**
     * <p><b>Assignment 2</b></p>
     * <p>
     *     Provide a Post-Processor which applies the following rules and add 'value' to the field:
     *     <ol>
     *         <li>Numbers should contain only one "." delimiter for decimal part with rounding up to two digits
     *         (for example, 6,633.66 → 6633.66).</li>
     *         <li>The currency sign should be removed. ISO 4217 code should be added after the number ($6633.66 → 6633.66 USD).
     *         Note that provided document contains only US dollar values.</li>
     *     </ol>
     * </p>
     *
     * As a template, use {@link Assignment2TotalPostProcessor}.
     */
    @Test
    public void assignment2() throws Exception {
        // Creates ML-SDK Document to process
        IeDocument document = getDocument("documents/lesson_4_assignment_2.html");
        // Adds Fields into document based on gold tagging
        addFields(document, Assignment2TotalPostProcessor.FIELD_NAME);

        // Process postprocessor
        processPostProcessor(document,
                new Assignment2TotalPostProcessor() //Assignment post-processor
        );

        // Gets all Fields provided by the Processor to check
        List<Field> fields = new ArrayList<>(document.findFields(Assignment2TotalPostProcessor.FIELD_NAME));

        // Checks the provided fields with the assignment's pattern
        checkElements(fields, "lesson_4_assignment_2_check.json");
    }


    /**
     * <p><b>Assignment 3</b></p>
     * <p>
     *     Implement a Post-Processor to filter out the wrong extracted values. Analyze the provided tagged document
     *     ml-sdk-lessons\lesson-4\documents\lesson_4_assignment_3.html. You should analyze the IBAN fields which are represented
     *     as HTML elements inside the {@code iban} tag.
     * </p>
     * <p>Tips:</p>
     * <ul>
     *     <li>Use the provided {@link IBANCheckDigit} class to implement Post-Processing.</li>
     *     <li>Use {@link Document#remove(Element element)} to remove wrong fields.</li>
     * </ul>
     *
     * As a template, use {@link Assignment3IBANPostProcessor}.
     */
    @Test
    public void assignment3() throws Exception {
        // Creates ML-SDK Document to process
        IeDocument document = getDocument("documents/lesson_4_assignment_3.html");
        // Adds Fields into document based on gold tagging
        addFields(document, Assignment3IBANPostProcessor.FIELD_NAME);

        // Process postprocessor
        processPostProcessor(document,
                new Assignment3IBANPostProcessor() //Assignment post-processor
        );

        // Gets all Fields provided by the Processor to check
        List<Field> fields = new ArrayList<>(document.findFields(Assignment3IBANPostProcessor.FIELD_NAME));

        // Checks the provided fields with the assignment's pattern
        checkElements(fields, "lesson_4_assignment_3_check.json");
    }

    /**
     * <p><b>Assignment 4</b></p>
     * <p>
     *     Provide a Post-Processor that handles the following modifications:
     *     <ol>
     *         <li>Converts prices from "$7,858.62" to "7858.62 USD" format. You can check the data-value attribute of each
     *         {@code price} field to analyze the expected result.</li>
     *         <li>Fix OCR errors in {@code price} according to the following pattern:</li>
     *         <ul>
     *             <li>Letters 'l', 'I', 'i' and vertical bar '|' with '1' (one).</li>
     *             <li>Letters 'G', 'b' with '6' (six).</li>
     *             <li>Letter 'B' with '8' (eight).</li>
     *             <li>Letter 'O' with '0' (zero).</li>
     *         </ul>
     *     </ol>
     * </p>
     *
     * As a template, use {@link Assignment4PricePostProcessor}.
     */
    @Test
    public void assignment4() throws Exception {
        // Creates ML-SDK Document to process
        IeDocument document = getDocument("documents/lesson_4_assignment_4.html");
        // Adds Fields into document based on gold tagging
        addFields(document, Assignment4PricePostProcessor.FIELD_NAME);

        // Process postprocessor
        processPostProcessor(document,
                new Assignment4PricePostProcessor() //Assignment post-processor
        );

        // Gets all Fields provided by the Processor to check
        List<Field> fields = new ArrayList<>(document.findFields(Assignment4PricePostProcessor.FIELD_NAME));

        // Checks the provided fields with the assignment's pattern
        checkElements(fields, "lesson_4_assignment_4_check.json");
    }

    /**
     * <p><b>Assignment 5</b></p>
     * <p>
     *     Provide a Post-Processor for restoring the original values from a dictionary in fields with name {@code product_type} that have
     *     been extracted with OCR errors. Use similarity score with a threshold of 0.8. Please update values from a dictionary only if
     *     the similarity score is greater than the threshold.
     * </p>
     * <p>Tips:</p>
     * <ul>
     *     <li>Use the OOTB solution {@link StringSimilarityUtils#jaroWinkler(String text, String gold)}.
     *     For more details, refer to <a href="https://github.com/tdebatty/java-string-similarity#jaro-winkler">Jaro-Winkler</a> documentation.</li>
     * </ul>
     *
     * As a template, use {@link Assignment5SimilarityPostProcessor}.
     */
    @Test
    public void assignment5() throws Exception {
        // Creates ML-SDK Document to process
        IeDocument document = getDocument("documents/lesson_4_assignment_5.html");
        // Adds Fields into document based on gold tagging
        addFields(document, Assignment5SimilarityPostProcessor.FIELD_NAME);

        // Process postprocessor
        processPostProcessor(document,
                new Assignment5SimilarityPostProcessor() //Assignment post-processor
        );

        // Gets all Fields provided by the Processor to check
        List<Field> fields = new ArrayList<>(document.findFields(Assignment5SimilarityPostProcessor.FIELD_NAME));

        // Checks the provided fields with the assignment's pattern
        checkElements(fields, "lesson_4_assignment_5_check.json");
    }

    /**
     * <p><b>Assignment 6</b></p>
     * <p>
     *     Provide a custom Post-Processor which will "repair" and expand partially tagged fields. Remove partially extracted fields from
     *     the {@link Document}, and then add the corrected ones expanded by the Post-Processor.
     * </p>
     * <p>Tips:</p>
     * <ul>
     *     <li>Use the {@link IeDocument#findCovering(Class searchClass, Element focusElement)} method to find the covering line.</li>
     *     <li>The newly created field should have the same score as the old field, as well as corrected {@code begin} and {@code end}
     *     positions, and value equal to the text of the whole line.</li>
     * </ul>
     *
     * As a template, use {@link Assignment6ExpandPostProcessor}.
     */
    @Test
    public void assignment6() throws Exception {
        // Creates ML-SDK Document to process
        IeDocument document = getDocument("documents/lesson_4_assignment_6.html");
        // Adds Fields into document based on gold tagging
        addFields(document, Assignment6ExpandPostProcessor.FIELD_NAME);

        // Process postprocessor
        processPostProcessor(document,
                new Assignment6ExpandPostProcessor() //Assignment post-processor
        );

        // Gets all Fields provided by the Processor to check
        List<Field> fields = new ArrayList<>(document.findFields(Assignment6ExpandPostProcessor.FIELD_NAME));

        // Checks the provided fields with the assignment's pattern
        checkElements(fields, "lesson_4_assignment_6_check.json");
    }

}