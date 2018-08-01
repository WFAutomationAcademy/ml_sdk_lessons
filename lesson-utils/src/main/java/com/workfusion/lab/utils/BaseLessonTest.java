/*
 * Copyright (C) WorkFusion 2018. All rights reserved.
 */
package com.workfusion.lab.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Permission;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.assertj.core.api.Condition;
import org.assertj.core.api.Java6Assertions;
import org.w3c.dom.Node;
import org.xmlunit.builder.Input;
import org.xmlunit.xpath.JAXPXPathEngine;

import com.workfusion.nlp.uima.api.parameter.sweeping.Dimension;
import com.workfusion.nlp.uima.pipeline.constants.ConfigurationConstants;
import com.workfusion.nlp.uima.pipeline.constants.PipelineConstants;
import com.workfusion.nlp.uima.util.JsonSerializationUtil;
import com.workfusion.nlp.uima.workflow.task.hpo.HpoConfiguration;
import com.workfusion.vds.nlp.model.configuration.ConfigurationBuilder;
import com.workfusion.vds.nlp.model.configuration.ConfigurationData;
import com.workfusion.vds.nlp.model.configuration.DefaultConfigurationContext;
import com.workfusion.vds.nlp.uima.model.UimaElementFactory;
import com.workfusion.vds.nlp.uima.model.cache.DefaultCacheBuilder;
import com.workfusion.vds.nlp.uima.model.lifecycle.LifecycleEventExecutor;
import com.workfusion.vds.sdk.api.nlp.annotator.Annotator;
import com.workfusion.vds.sdk.api.nlp.configuration.FieldInfo;
import com.workfusion.vds.sdk.api.nlp.configuration.FieldType;
import com.workfusion.vds.sdk.api.nlp.configuration.IllegalConfigurationException;
import com.workfusion.vds.sdk.api.nlp.fe.Feature;
import com.workfusion.vds.sdk.api.nlp.fe.FeatureExtractor;
import com.workfusion.vds.sdk.api.nlp.model.Cell;
import com.workfusion.vds.sdk.api.nlp.model.Document;
import com.workfusion.vds.sdk.api.nlp.model.Element;
import com.workfusion.vds.sdk.api.nlp.model.Field;
import com.workfusion.vds.sdk.api.nlp.model.IeDocument;
import com.workfusion.vds.sdk.api.nlp.model.Line;
import com.workfusion.vds.sdk.api.nlp.model.NamedEntity;
import com.workfusion.vds.sdk.api.nlp.model.Row;
import com.workfusion.vds.sdk.api.nlp.model.Table;
import com.workfusion.vds.sdk.api.nlp.model.Tag;
import com.workfusion.vds.sdk.api.nlp.model.Token;
import com.workfusion.vds.sdk.api.nlp.processing.Processor;
import com.workfusion.vds.sdk.nlp.component.util.DocumentFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class BaseLessonTest {

    /**
     * Logger for messaging
     */
    protected static final Log logger = LogFactory.getLog("TRAINING");

    /**
     * Helper method: creates default configuration context.
     *
     * @return
     */
    protected static DefaultConfigurationContext getConfigurationContext() {
        return new DefaultConfigurationContext(getTestFieldInfo(), new HashMap<>());
    }

    /**
     * Helper method: creates a default FieldInfo.
     */
    protected static FieldInfo getTestFieldInfo() {
        return new FieldInfo.Builder("test")
                .type(FieldType.FREE_TEXT)
                .build();
    }

    /**
     * Helper method: returns a configuration dimension from the specified model configuration.
     */
    protected ConfigurationData buildConfiguration(Class<?> configurationClass) {
        return buildConfiguration(configurationClass, getConfigurationContext());
    }

    /**
     * Helper method: returns a configuration dimension from the specified model configuration and context.
     */
    protected ConfigurationData buildConfiguration(Class<?> configurationClass, DefaultConfigurationContext context) {
        log("Checking the configuration {0} class for field {1}", configurationClass.getSimpleName(), context.getField().getCode());
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder(configurationClass);
        ConfigurationData configurationData = configurationBuilder.buildConfiguration(context);
        log("Checking the defined configuration.");
        assertThat(configurationData).isNotNull();
        return configurationData;
    }

    /**
     * Helper method: returns a configuration dimension from the specified model configuration and field info.
     */
    protected ConfigurationData buildConfiguration(Class<?> configurationClass, FieldInfo fieldInfo) {
        return buildConfiguration(configurationClass, new DefaultConfigurationContext(fieldInfo, new HashMap<>()));
    }

    /**
     * Helper method: returns annotators from dimension.
     */
    protected List<Annotator> getAnnotatorsFromConfiguration(ConfigurationData configurationData, int expectedSize) {
        ListMultimap<String, Dimension> dimensions = configurationData.getParameterSpace().getDimensions();
        // Obtains defined annotators list.
        List<Annotator> annotators = null;
        try {
            annotators = (List<Annotator>) getDimensionParameters(dimensions, "annotators");
        } catch (Exception e) {
            log("There is no @Named returns List<Annotator<Document>> in the ModelConfiguration class.");
        }
        log("Checking that annotators are defined in @Named methods.");
        assertThat(annotators).isNotNull();
        if (expectedSize > -1) {
            assertThat(annotators.size()).isEqualTo(expectedSize);
        }
        return annotators;
    }


    /**
     * Helper method: returns FEs from dimension.
     */
    protected List<FeatureExtractor> getFEsFromConfiguration(ConfigurationData configurationData, int expectedSize) {
        ListMultimap<String, Dimension> dimensions = configurationData.getParameterSpace().getDimensions();
        // Obtains defined annotators list.
        List<FeatureExtractor> fes = null;
        try {
            fes = (List<FeatureExtractor>) getDimensionParameters(dimensions, "FE");
        } catch (Exception e) {
            log("There is no @Named returns List<FeatureExtractors<Document>> in the ModelConfiguration class.");
        }
        log("Checking that FEs are defined in @Named methods.");
        assertThat(fes).isNotNull();
        if (expectedSize > -1) {
            assertThat(fes.size()).isEqualTo(expectedSize);
        }
        return fes;
    }

    /**
     * Helper method: returns annotators from dimension.
     */
    protected List<Processor> getProcessorsFromConfiguration(ConfigurationData configurationData, int expectedSize) {
        // Obtains defined annotators list.
        List<Processor> processors = null;
        try {
            processors = (List<Processor>) configurationData.getPostProcessors();
        } catch (Exception e) {
            log("There is no @Named returns List<Processor<IeDocument>> in the ModelConfiguration class.");
        }
        log("Checking that post-processors are defined in @Named methods.");
        assertThat(processors).isNotNull();
        if (expectedSize > -1) {
            assertThat(processors.size()).isEqualTo(expectedSize);
        }
        return processors;
    }

    /**
     * Helper method: returns configuration parameters from dimension.
     */
    private List<?> getDimensionParameters(ListMultimap<String, Dimension> dimensions, String name) {
        List<Object> result = new ArrayList<>();
        for (Dimension dim : dimensions.asMap().get(name)) {
            result.addAll(((List<Object>) dim.getParameters().stream()
                    .map(p -> p.getValue())
                    .findFirst()
                    .get()));
        }
        return result;
    }

    /**
     * Calls annotator method marked by @OnInit to initialize the annotator if needed.
     */
    protected void initializeAnnotator(Annotator annotator) {
        LifecycleEventExecutor.getInstance().executeInit(annotator, Collections.emptyMap());
    }

    /**
     * Calls fe's method marked by @OnInit to initialize the fe if needed.
     */
    protected void initializeFe(FeatureExtractor fe) {
        Class focusElementClass = UimaElementFactory.getInstance().findElementType(org.cleartk.token.type.Token.class);
        DefaultCacheBuilder indexBuilder = new DefaultCacheBuilder();
        LifecycleEventExecutor.getInstance().executeInit(fe, Collections.emptyMap(), indexBuilder, focusElementClass);
    }

    /**
     * Helper method: Creates ML-SDK document from the provided file path.
     *
     * @param documentPath - the document path
     * @return ML-SDK document
     * @throws IOException is the file doesn't exist
     */
    protected IeDocument getDocument(String documentPath) throws IOException {
        log("Loading document {0} for checking ...", documentPath);
        Path path = Paths.get(documentPath);
        String content = new String(Files.readAllBytes(Paths.get(documentPath)), StandardCharsets.UTF_8)
                .replaceAll("(\\r\\n|\\r|\\n)+", "\r\n");

        IeDocument document;
        if (FileSystems.getDefault().getPathMatcher("glob:**.html").matches(path)) {
            DocumentParser formatter = new DocumentParser();
            DocumentParser.DocumentContent documentContent = formatter.prepareDocumentContent(content);
            document = DocumentFactory.createIeDocument(content, documentContent.getText());
            documentContent.getTags().forEach(t -> {
                Tag.Descriptor tagDescriptor = Tag.descriptor()
                        .setBegin(t.getBegin())
                        .setEnd(t.getEnd())
                        .setName(t.getName());
                t.getAttr().entrySet().forEach(e -> {
                            tagDescriptor.setAttribute(e.getKey(), e.getValue());
                        }
                );
                document.add(tagDescriptor);

                if (t.getName().equalsIgnoreCase("table")) {
                    document.add(Table.descriptor()
                            .setBegin(t.getBegin())
                            .setEnd(t.getEnd())
                    );
                } else if (t.getName().equalsIgnoreCase("tr")) {
                    document.add(Row.descriptor()
                            .setBegin(t.getBegin())
                            .setEnd(t.getEnd())
                            .setRowIndex(Integer.parseInt(t.getAttr().get(DocumentParser.ROW_INDEX_ATTR)))
                    );
                } else if (t.getName().equalsIgnoreCase("th") || t.getName().equalsIgnoreCase("td")) {
                    document.add(Cell.descriptor()
                            .setBegin(t.getBegin())
                            .setEnd(t.getEnd())
                            .setRowIndex(Integer.parseInt(t.getAttr().get(DocumentParser.ROW_INDEX_ATTR)))
                            .setColumnIndex(Integer.parseInt(t.getAttr().get(DocumentParser.COLUMN_INDEX_ATTR)))
                    );
                } else if (t.getName().equalsIgnoreCase("line")) {
                    document.add(Line.descriptor()
                            .setBegin(t.getBegin())
                            .setEnd(t.getEnd())
                            .setLineIndex(Integer.parseInt(t.getAttr().get(DocumentParser.LINE_INDEX_ATTR)))
                    );
                }
            });

        } else {
            document = DocumentFactory.createIeDocument(content, content);
        }

        return document;
    }

    /**
     * Adds Fields into document based on gold tagging.
     */
    protected void addFields(Document document, String... fields) {
        List<String> ids = Arrays.asList(fields);
        Collection<Tag> tags = document.findAll(Tag.class);
        tags.stream().forEach(t -> {
                    if (ids.contains(t.getName())) {
                        Field.Descriptor tagDescriptor = Field.descriptor()
                                .setBegin(t.getBegin())
                                .setEnd(t.getEnd())
                                .setName(t.getName())
                                .setScore(0.5)
                                .setValue(t.getText());
                        t.getAttributes().entrySet().forEach(e -> {
                                    tagDescriptor.setAttribute(e.getKey(), e.getValue());
                                }
                        );
                        document.add(tagDescriptor);
                    }
                }
        );
    }

    /**
     * Helper method: Outputs formated log into console
     *
     * @param message the log format
     * @param params  the list of objects to output
     */
    protected void log(String message, Object... params) {
        logger.info(new MessageFormat(message).format(params));
    }

    /**
     * Helper method: Checks the provided elements with the pattern
     *
     * @param elements    the element list
     * @param patternFile the pattern JSON file to load and check
     */
    protected void checkElements(List<? extends Element> elements, String patternFile) throws IOException {
        log("Checking provided document elements ...");

        // Loads token check patterns
        List<Map<String, Object>> patterns = (List<Map<String, Object>>) JsonSerializationUtil.readObject(this.getClass()
                .getResourceAsStream(patternFile));

        for (int i = 0; i < Math.min(elements.size(), patterns.size()); i++) {
            log("{0}:{1}", elements.get(i).getClass().getSimpleName().substring(4), i);
            Map<String, Object> pattern = patterns.get(i);

            log("\ttext: \"{0}\" <-- \"{1}\"", elements.get(i).getText(), pattern.get("text"));
            assertThat(elements.get(i).getText()).isEqualTo(pattern.get("text"));

            log("\tbegin: {0} <-- {1}", elements.get(i).getBegin(), pattern.get("begin"));
            assertThat(elements.get(i).getBegin()).isEqualTo(pattern.get("begin"));

            log("\tend: {0} <-- {1}", elements.get(i).getEnd(), pattern.get("end"));
            assertThat(elements.get(i).getEnd()).isEqualTo(pattern.get("end"));

            if (elements.get(i) instanceof NamedEntity) {
                log("\ttype: \"{0}\" <-- \"{1}\"", ((NamedEntity) elements.get(i)).getType(), pattern.get("type"));
                assertThat(((NamedEntity) elements.get(i)).getType()).isEqualTo(pattern.get("type"));
            }
        }
        log("Checking number of provided elements size: {0} <-- {1}", elements.size(), patterns.size());
        assertThat(elements.size()).isEqualTo(patterns.size());
    }

    /**
     * Helper method: Checks the provided element features with the pattern
     *
     * @param providedElementFeatures the element->feature set
     * @param patternFile             the pattern JSON file to load and check
     */
    protected void checkElementFeatures(Map<Element, Set<Feature>> providedElementFeatures, String patternFile) throws IOException {
        log("Checking provided element features ...");

        // Loads FE check patterns
        List<Map<String, Object>> patterns = (List<Map<String, Object>>) JsonSerializationUtil.readObject(this.getClass()
                .getResourceAsStream(patternFile));

        List<Map.Entry<Element, Set<Feature>>> providedEntries = providedElementFeatures.entrySet().stream().collect(Collectors.toList());

        for (int i = 0; i < Math.min(providedEntries.size(), patterns.size()); i++) {
            Element element = providedEntries.get(i).getKey();
            List<Feature> elementFeatures = providedEntries.get(i).getValue().stream().collect(Collectors.toList());

            Map<String, Object> pattern = patterns.get(i);
            List<Feature> patternFeatures = ((Set<Feature>) pattern.get("features")).stream().collect(Collectors.toList());

            if (!(elementFeatures.size() == patternFeatures.size() && patternFeatures.size() == 0)) {
                log("\t{0}:{1}, \"{2}\", {3}-{4}",
                        element.getClass().getSimpleName().substring(4),
                        i,
                        element.getText(),
                        element.getBegin(),
                        element.getEnd());
                log("\t\tChecking the number of provided features.");
                assertThat(elementFeatures.size()).isEqualTo(patternFeatures.size());

                for (int j = 0; j < Math.min(patternFeatures.size(), elementFeatures.size()); j++) {

                    log("\t\tfeature:{0}, name: \"{1}\" <-- \"{2}\"",
                            j,
                            elementFeatures.get(j).getName(),
                            patternFeatures.get(j).getName());
                    assertThat(elementFeatures.get(j).getName()).isEqualTo(patternFeatures.get(j).getName());
                }
            }

        }
        log("\tChecking number of checked tokens: {0} <-- {1}", providedEntries.size(), patterns.size());
        assertThat(providedEntries.size()).isEqualTo(patterns.size());

    }

    /**
     * Helper method: Checks the provided fields with the pattern
     *
     * @param fields      the element list
     * @param patternFile the pattern JSON file to load and check
     */
    protected void checkFields(List<? extends Field> fields, String patternFile) throws IOException {
        log("Checking provided document fields ...");

        // Loads processor check patterns
        List<Map<String, Object>> patterns = (List<Map<String, Object>>) JsonSerializationUtil.readObject(this.getClass()
                .getResourceAsStream(patternFile));

        for (int i = 0; i < Math.min(fields.size(), patterns.size()); i++) {
            log("{0}:{1}", fields.get(i).getClass().getSimpleName().substring(4), i);
            Map<String, Object> pattern = patterns.get(i);

            log("\tname: \"{0}\" <-- \"{1}\"", fields.get(i).getName(), pattern.get("name"));
            assertThat(fields.get(i).getName()).isEqualTo(pattern.get("name"));

            log("\ttext: \"{0}\" <-- \"{1}\"", fields.get(i).getText(), pattern.get("text"));
            assertThat(fields.get(i).getText()).isEqualTo(pattern.get("text"));

            log("\tvalue: \"{0}\" <-- \"{1}\"", fields.get(i).getValue(), pattern.get("value"));
            assertThat(fields.get(i).getValue()).isEqualTo(pattern.get("value"));

            log("\tbegin: {0} <-- {1}", fields.get(i).getBegin(), pattern.get("begin"));
            assertThat(fields.get(i).getBegin()).isEqualTo(pattern.get("begin"));

            log("\tend: {0} <-- {1}", fields.get(i).getEnd(), pattern.get("end"));
            assertThat(fields.get(i).getEnd()).isEqualTo(pattern.get("end"));

            log("\tscore: {0} <-- {1}", fields.get(i).getScore(), pattern.get("score"));
            assertThat(fields.get(i).getScore()).isEqualTo(pattern.get("score"));

        }
        log("Checking number of provided fields size: {0} <-- {1}", fields.size(), patterns.size());
        assertThat(fields.size()).isEqualTo(patterns.size());
    }

    /**
     * Serializes elements list into a file
     *
     * @param elements elements to serilize
     * @param file     the file name
     * @throws IOException
     */
    protected void writeElements(Collection<? extends Element> elements, String file) throws IOException {
        final List<Map<String, Object>> ser = new ArrayList<>();
        elements.stream().forEach((t) -> {
                    Map<String, Object> res = new HashMap<String, Object>();
                    res.put("begin", t.getBegin());
                    res.put("end", t.getEnd());
                    res.put("text", t.getText());
                    if (t instanceof NamedEntity) {
                        res.put("type", ((NamedEntity) t).getType());
                    }
                    ser.add(res);
                }
        );
        JsonSerializationUtil.writeObject(new File(file), ser);
    }

    /**
     * Serializes element features list into a file
     *
     * @param elements
     * @param file
     * @throws IOException
     */
    protected void writeElementFeatures(Map<? extends Element, Set<Feature>> elements, String file) throws IOException {
        final List<Map<String, Object>> ser = new ArrayList<>();
        elements.entrySet().stream().forEach(entry -> {
                    Element element = entry.getKey();
                    Map<String, Object> res = new HashMap<String, Object>();
                    res.put("begin", element.getBegin());
                    res.put("end", element.getEnd());
                    res.put("text", element.getText());
                    if (element instanceof NamedEntity) {
                        res.put("type", ((NamedEntity) element).getType());
                    }
                    res.put("features", entry.getValue());
                    ser.add(res);
                }
        );
        JsonSerializationUtil.writeObject(new File(file), ser);
    }


    /**
     * Serializes fields list into a file
     *
     * @param fields elements to serilize
     * @param file   the file name
     * @throws IOException
     */
    protected void writeFields(Collection<? extends Field> fields, String file) throws IOException {
        final List<Map<String, Object>> ser = new ArrayList<>();
        fields.stream().forEach((t) -> {
                    Map<String, Object> res = new HashMap<String, Object>();
                    res.put("begin", t.getBegin());
                    res.put("end", t.getEnd());
                    res.put("text", t.getText());
                    res.put("name", t.getName());
                    res.put("value", t.getValue());
                    res.put("score", t.getScore());
                    ser.add(res);
                }
        );
        JsonSerializationUtil.writeObject(new File(file), ser);
    }

    /**
     * Helper method: process FE list for the document
     */
    protected Map<Element, Set<Feature>> processFeatures(Document document,
            FeatureExtractor... fes) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        return processFeatures(document, Arrays.asList(fes));
    }

    /**
     * Helper method: process FE list for the document
     */
    protected Map<Element, Set<Feature>> processFeatures(Document document,
            List<FeatureExtractor> fes) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        return processFeatures(document, document, fes, false);
    }

    /**
     * Returns an non accessible document to check FE's lifecycle
     */
    protected Document getNotAccessibleDocument() {
        return new Document() {
            @Override
            public String getId() {
                noAccess();
                return null;
            }

            @Override
            public String getText() {
                noAccess();
                return null;
            }

            @Override
            public String getContent() {
                noAccess();
                return null;
            }

            @Override
            public <T extends Element> T add(Element.ElementDescriptor descriptor) {
                noAccess();
                return null;
            }

            @Override
            public void remove(Element element) {
                noAccess();

            }

            @Override
            public void removeAll() {
                noAccess();

            }

            @Override
            public Collection<Element> findAll() {
                noAccess();
                return null;
            }

            @Override
            public <T extends Element> Collection<T> findAll(Class<T> type) {
                noAccess();
                return null;
            }

            @Override
            public <T extends Element> List<T> findCovered(Class<T> type, Element coveringElement) {
                noAccess();
                return null;
            }

            @Override
            public <T extends Element> List<T> findCovered(Class<T> type, int begin, int end) {
                noAccess();
                return null;
            }

            @Override
            public <T extends Element> List<T> findCovering(Class<T> type, Element coveredElement) {
                noAccess();
                return null;
            }

            @Override
            public <T extends Element> List<T> findCovering(Class<T> type, int begin, int end) {
                noAccess();
                return null;
            }

            @Override
            public <T extends Element> List<T> findNext(Class<T> type, Element anchor, int count) {
                noAccess();
                return null;
            }

            @Override
            public <T extends Element> List<T> findPrevious(Class<T> type, Element anchor, int count) {
                noAccess();
                return null;
            }

            @Override
            public <T extends Element, S extends Element> Map<T, Collection<S>> findAllCovered(Class<? extends T> type,
                    Class<? extends S> coveredType) {
                noAccess();
                return null;
            }

            @Override
            public <T extends Element, S extends Element> Map<T, Collection<S>> findAllCovering(Class<? extends T> type,
                    Class<? extends S> coveringType) {
                noAccess();
                return null;
            }

            private void noAccess() {
                log("The document is not accessible from the current point according to assignment requirements!");
                throw new AssertionError("The document is not accessible from the current point according to assignment requirements!");
            }
        };
    }

    /**
     * Helper method: process FE list for the document
     *
     * @param onStartDocument document to use for FEs initialization
     * @param processDocument document to use for FEs process
     * @param fes             the feature extractor list to process
     * @return the features map to check
     */
    protected Map<Element, Set<Feature>> processFeatures(Document onStartDocument,
            Document processDocument,
            FeatureExtractor... fes) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        return processFeatures(onStartDocument, processDocument, Arrays.asList(fes), false);
    }

    /**
     * Helper method: process FE list for the document
     *
     * @param onStartDocument document to use for FEs initialization
     * @param processDocument document to use for FEs process
     * @param fes             the feature extractor list to process
     * @return the features map to check
     */
    protected Map<Element, Set<Feature>> processFeatures(Document onStartDocument,
            Document processDocument,
            List<FeatureExtractor> fes,
            boolean restrictTokenAccess) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        log("Feature extractors initialization ...");
        Class focusElementClass = UimaElementFactory.getInstance().findElementType(org.cleartk.token.type.Token.class);
        DefaultCacheBuilder indexBuilder = new DefaultCacheBuilder();
        for (FeatureExtractor fe : fes) {
            log("\t{0}::Init", fe.getClass().getName());
            LifecycleEventExecutor.getInstance().executeInit(fe, Collections.emptyMap(), indexBuilder, focusElementClass);

            log("\t{0}::OnDocumentStart", fe.getClass().getName());
            LifecycleEventExecutor.getInstance().executeOnDocumentStart(fe, onStartDocument, focusElementClass);
        }

        log("Applying feature extractor to Tokens.");
        // Gets all tokens from document to process
        List<Token> tokens = onStartDocument.findAll(Token.class).stream().collect(Collectors.toList());

        // Gives all features provided by custom FEs
        Map<Element, Set<Feature>> providedElementFeatures = new LinkedHashMap<>();
        // Restrict access to Token TEXT
        List<String> tokenText = new ArrayList<>();
        if (restrictTokenAccess) {
            for (Token token : tokens) {
                tokenText.add(token.getText());
                FieldUtils.writeField(token, "text", "NO ACCESS", true);
            }
        }
        for (FeatureExtractor fe : fes) {
            log("\t{0}::extract", fe.getClass().getName());
            for (Token token : tokens) {
                if (restrictTokenAccess) {
                    tokenText.add(token.getText());
                    FieldUtils.writeField(token, "text", "NO ACCESS", true);
                }
                Collection<Feature> features = fe.extract(processDocument, token);
                Collection<Feature> tokenFeatures = providedElementFeatures.computeIfAbsent(token, k -> new HashSet());
                tokenFeatures.addAll(features);
            }
        }
        // Return text back for Token's
        if (restrictTokenAccess) {
            for (int i = 0; i < tokens.size(); i++) {
                FieldUtils.writeField(tokens.get(i), "text", tokenText.get(i), true);
            }
        }

        log("Feature extractors completion ...");
        for (FeatureExtractor fe : fes) {

            log("\t{0}::OnDocumentComplete", fe.getClass().getName());
            LifecycleEventExecutor.getInstance().executeOnDocumentComplete(fe, Collections.emptyMap());

            log("\t{0}::Destroy", fe.getClass().getName());
            LifecycleEventExecutor.getInstance().executeDestroy(fe, Collections.emptyMap());
        }

        return providedElementFeatures;
    }


    /**
     * Helper method: process annotators list for the document
     *
     * @param document   the document to process
     * @param annotators the annotators list to process
     */
    protected void processAnnotators(Document document, Annotator... annotators) {
        processAnnotators(document, Arrays.asList(annotators));
    }

    /**
     * Helper method: process annotators list for the document
     *
     * @param document   the document to process
     * @param annotators the annotators list to process
     */
    protected void processAnnotators(Document document, List<Annotator> annotators) {
        log("Annotators initialization ...");
        for (Annotator annotator : annotators) {
            log("\t{0}::Init", annotator.getClass().getName());
            LifecycleEventExecutor.getInstance().executeInit(annotator, Collections.emptyMap());
        }

        // Call the annotator.process method for the all annotators defined in the configuration
        log("Applying annotators to the document.");
        for (Annotator annotator : annotators) {
            log("\t{0}:process", annotator.getClass().getName());
            annotator.process(document);
        }

        log("Annotators completion ...");
        for (Annotator annotator : annotators) {
            log("\t{0}::Destroy", annotator.getClass().getName());
            LifecycleEventExecutor.getInstance().executeDestroy(annotator, Collections.emptyMap());
        }
    }

    /**
     * Checks that Tokens have been provided by annotators.
     */
    protected void checkTokenAreProvided(Document document) {
        log("Checking that Tokens have been provided by annotators.");
        assertThat(document.findAll(Token.class).size()).isGreaterThan(0);
    }

    /**
     * Helper method: process processors list for the document
     *
     * @param document   the document to process
     * @param processors the processors list to use
     */
    protected void processPostProcessor(Document document, Processor... processors) {
        processPostProcessor(document, Arrays.asList(processors));
    }

    /**
     * Helper method: process processors list for the document
     *
     * @param document   the document to process
     * @param processors the processors list to use
     */
    protected void processPostProcessor(Document document, List<Processor> processors) {
        log("Processors initialization ...");
        for (Processor processor : processors) {
            log("\t{0}::Init", processor.getClass().getName());
            LifecycleEventExecutor.getInstance().executeInit(processor, Collections.emptyMap());
        }

        // Call the annotator.process method for the all annotators defined in the configuration
        log("Applying processor to the document.");
        for (Processor processor : processors) {
            log("\t{0}:process", processor.getClass().getName());
            processor.process(document);
        }

        log("Processor completion ...");
        for (Processor processor : processors) {
            log("\t{0}::Destroy", processor.getClass().getName());
            LifecycleEventExecutor.getInstance().executeDestroy(processor, Collections.emptyMap());
        }
    }

    /**
     * Security manager class implementation to prevent System.exit()
     */
    private static class PreventSystemExitSecurityManager extends SecurityManager {

        private final SecurityManager _prevMgr;

        public PreventSystemExitSecurityManager(final SecurityManager parent) {
            this._prevMgr = parent;
        }

        public PreventSystemExitSecurityManager() {
            this(System.getSecurityManager());
        }


        @Override
        public void checkPermission(final Permission perm) {
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
        }

        @Override
        public void checkExit(final int code) {
            throw new PreventSystemExitException();
        }

        public SecurityManager getPreviousMgr() { return _prevMgr; }

        public static class PreventSystemExitException extends RuntimeException {
        }

        public static void disableSystemExit(boolean disabled) {
            if (disabled) {
                System.setSecurityManager(new PreventSystemExitSecurityManager());
            } else {
                SecurityManager mgr = System.getSecurityManager();
                if ((mgr != null) && (mgr instanceof PreventSystemExitSecurityManager)) {
                    PreventSystemExitSecurityManager smgr = (PreventSystemExitSecurityManager) mgr;
                    System.setSecurityManager(smgr.getPreviousMgr());
                } else {
                    System.setSecurityManager(null);
                }
            }
        }
    }

    /**
     * Execute a runner class.
     */
    protected void executeRunner(Class<?> runnerClass) throws Exception {
        log("Executing the runner {0} ...", runnerClass);
        PreventSystemExitSecurityManager.disableSystemExit(true);
        try {
            Method main = runnerClass.getDeclaredMethod("main", new Class[] {String[].class});
            String[] mainArgs = new String[] {};
            main.invoke(null, (Object) mainArgs);

        } catch (PreventSystemExitSecurityManager.PreventSystemExitException e) {
            // Do nothing
        } finally {
            PreventSystemExitSecurityManager.disableSystemExit(false);
        }
    }

    /**
     * Input directory path to use.
     */
    public final static String TEST_INPUT_DIR_PATH = "data/test/";
    /**
     * Output directory path to use.
     */
    public final static String EXTRACT_OUTPUT_DIR_PATH = "results/extract/";

    /**
     * Input directory path to use.
     */
    public final static String TRAIN_INPUT_DIR_PATH = "data/train/";
    /**
     * Output directory path to use.
     */
    public final static String TRAINING_OUTPUT_DIR_PATH = "results/training/";

    /**
     * Model directory path to use.
     */
    public final static String MODEL_DIR_PATH = TRAINING_OUTPUT_DIR_PATH + "output/model/";

    /**
     * Field statistics wrapper
     */
    public static class FieldStatistic {

        double precision;
        double recall;

        public FieldStatistic(double precision, double recall) {
            this.precision = precision;
            this.recall = recall;
        }
    }

    /**
     * Returns field statistics map to check for the specified file.
     */
    private Map<String, FieldStatistic> getFieldStatisticsFromFile(String path) {

        Map<String, FieldStatistic> result = new HashMap<>();
        try {
            if (path.endsWith(PipelineConstants.AVG_EVALUATION_RESULTS_FILE_NAME)) {
                FileReader fileReader = new FileReader(path);
                CSVReader reader = new CSVReader(fileReader, '\t');
                reader.readNext(); //skip headers
                reader.forEach(csvLine -> {
                            result.put(csvLine[0],
                                    new FieldStatistic(Double.parseDouble(csvLine[4]), Double.parseDouble(csvLine[5])));
                        }
                );

            } else if (path.endsWith("per-field-statistics.csv")) {
                FileReader fileReader = new FileReader(path);
                CSVReader reader = new CSVReader(fileReader, ',');
                reader.readNext(); //skip headers
                reader.forEach(csvLine -> {
                            try {
                                result.put(csvLine[0],
                                        new FieldStatistic(Double.parseDouble(csvLine[5]), Double.parseDouble(csvLine[6])));
                            } catch (NumberFormatException e) {
                            }
                        }
                );
            } else {
                log("Model SDK version is not supported. Please use 9.0.0.19 or compatible.");
            }
        } catch (Exception e) {
        }
        return result;
    }

    /**
     * Returns model training field statistics,
     */
    protected Map<String, FieldStatistic> getTrainingFieldStatistics(String outputDir) {
        String path = outputDir + "/training/output/" + PipelineConstants.AVG_EVALUATION_RESULTS_FILE_NAME;
        log("Obtaining the model training statistics file to check the model training is completed successfully ({0}) ...", path);
        Map<String, FieldStatistic> result = getFieldStatisticsFromFile(path);
        assertThat(result.size()).isGreaterThan(0);
        return result;
    }

    /**
     * Returns model extract field statistics,
     */
    protected Map<String, FieldStatistic> getExecutionFieldStatistics(String statsDir) {
        String path = null;
        log("Finding the the model execution statistics file path to check ...");
        try {
            Optional<Path> trainedStatisticsFile = StreamSupport.stream(
                    Files.newDirectoryStream(Paths.get(statsDir), "stats_*").spliterator(),
                    false).sorted((p1, p2) -> (int) (p2.toFile().lastModified() - p1.toFile().lastModified())).findFirst();
            path = trainedStatisticsFile.get().toFile().getAbsolutePath() + "/per-field-statistics.csv";
        } catch (Exception ioe) {
        }
        assertThat(path).isNotNull();
        log("Obtaining the model execution statistics file to check the model execution is completed successfully ({0}) ...", path);
        Map<String, FieldStatistic> result = getFieldStatisticsFromFile(path);
        assertThat(result.size()).isGreaterThan(0);
        return result;

    }

    /**
     * Checks field statistics.
     */
    protected void checkFieldStatistics(Map<String, FieldStatistic> statistic, String field, double minPrecision, double minRecall) {
        log("Checking field \"{0}\" statistics:", field);
        assertThat(statistic.get(field)).isNotNull();
        FieldStatistic s = statistic.get(field);
        log("\tprecision {0} >= {1}", s.precision, minPrecision);
        assertThat(s.precision).isGreaterThanOrEqualTo(minPrecision);
        log("\trecall {0} >= {1}", s.recall, minRecall);
        assertThat(s.recall).isGreaterThanOrEqualTo(minRecall);
    }

    /**
     * Checks the annotator in the provided list.
     */
    protected void checkAnnotatorInList(List<Annotator> annotators, Class<? extends Annotator> annotator, int index) {
        log("Checking the {0} annotator is {1}.", index, annotator);
        assertThat(annotators.get(index).getClass()).isEqualTo(annotator);
    }

    /**
     * Checks the FE in the provided list.
     */
    protected void checkFeInList(List<FeatureExtractor> fes, Class<? extends FeatureExtractor>... requiredFes) {
        for (Class<? extends FeatureExtractor> requiredFe : requiredFes) {
            log("Checking the {0} FE is present ...", requiredFe);
            boolean present = fes.stream().filter(f -> f.getClass().equals(requiredFe)).count() > 0;
            assertThat(present).isEqualTo(true);
        }
    }

    /**
     * Checks the HPO helper methods
     */
    protected File getHpoConfigFileForIESubmodel(String traingPath, String submodelName) {
        return new File(traingPath, "training/work/pre-eval/" + submodelName + "/hpo-config.json");
    }

    protected HpoConfiguration getHpoConfiguration(File hpoConfigFile) {
        ExclusionStrategy excludeInterfaceFields = new FieldsExclusionStrategy();
        Gson gson = new GsonBuilder()
                .setExclusionStrategies(excludeInterfaceFields)
                .create();
        try {
            return gson.fromJson(new FileReader(hpoConfigFile), HpoConfiguration.class);
        } catch (FileNotFoundException e) {
            fail("Cannot find configuration file " + hpoConfigFile + " " + e);
        }
        return null;
    }

    private static class FieldsExclusionStrategy implements ExclusionStrategy {

        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }

        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
            return fieldAttributes.getDeclaredClass().isInterface();
        }
    }

    private Map<String, Object> loadConfigurationFromFile(File configurationFile) {
        try (InputStream stream = new BufferedInputStream(new FileInputStream(configurationFile))) {
            return (Map<String, Object>) JsonSerializationUtil.readObject(stream);
        } catch (IOException e) {
            throw new IllegalConfigurationException(e);
        }
    }

    protected void assertPostProcessors(String workingDir, String expectedPostProcessorsString) {
        log("Checking the {0} was added to config.", "ExpandPostProcessor");
        File pipelineAE = new File(workingDir + "/training/output/model/pipeline-AE.xml");
        assertThat(pipelineAE).exists();
        JAXPXPathEngine engine = new JAXPXPathEngine();
        engine.setNamespaceContext(ImmutableMap.of("ns", "http://uima.apache.org/resourceSpecifier"));
        Iterable<Node> nodesIterator = engine.selectNodes(
                "//ns:delegateAnalysisEngine[@key='Post-Processing Annotator']//ns:nameValuePair/*[contains(., 'items')]",
                Input.fromFile(pipelineAE).build());

        assertThat(nodesIterator).hasSize(1);

        String nodeValue = nodesIterator.iterator().next().getTextContent();
        assertThat(nodeValue).contains(expectedPostProcessorsString);
    }

    protected void assertFieldInvoiceNumberFe(String outputPath, String fieldName, Class<? extends FeatureExtractor> featureExtractor) {
        Map<String, Object> parametersJson = loadConfigurationFromFile(new File(outputPath + "/training/output/model/" + fieldName + "/config/" + PipelineConstants.PARAMETERS_FILE_NAME));
        List<FeatureExtractor> featureExtractors = (List<FeatureExtractor>) parametersJson.get(ConfigurationConstants.FEATURE_EXTRACTORS);
        log("Checking the {0} presents for field {1}.", featureExtractor.getName(), fieldName);
        Condition<FeatureExtractor> featureExtractorForInvoice = new Condition<>(fe -> fe.getClass().isAssignableFrom(featureExtractor), "");
        Java6Assertions.assertThat(featureExtractors).areExactly(1, featureExtractorForInvoice);
        assertThat(featureExtractors.size()).isGreaterThan(1);
    }

    protected void assertFieldProductFe(String outputPath, String fieldName, Class<? extends FeatureExtractor>... featureExtractor) {
        log("Checking the {0} not presents for field {1}.", featureExtractor[0].getName(), fieldName);
        Map<String, Object> parametersJson = loadConfigurationFromFile(new File(outputPath + "/training/output/model/" + fieldName + "/config/" + PipelineConstants.PARAMETERS_FILE_NAME));
        List<FeatureExtractor> featureExtractors = (List<FeatureExtractor>) parametersJson.get(ConfigurationConstants.FEATURE_EXTRACTORS);
        Condition<FeatureExtractor> featureExtractorForInvoice = new Condition<>(fe -> fe.getClass().isAssignableFrom(featureExtractor[0]), "");
        Java6Assertions.assertThat(featureExtractors).areNot(featureExtractorForInvoice);
        log("Checking the {0} or {1} presents for field {2}.", featureExtractor[1].getName(), featureExtractor[2].getName(), fieldName);
        assertThat(featureExtractors.size()).isGreaterThan(1);
        Condition<FeatureExtractor> featureExtractorForProduct = new Condition<>(fe -> fe.getClass().isAssignableFrom(featureExtractor[1]) || fe.getClass().isAssignableFrom(featureExtractor[2]), "");
        assertThat(featureExtractors).areExactly(1, featureExtractorForProduct);

    }

    protected void assertHpoConstant(HpoConfiguration hpoConfiguration) {
        log("Checking the HPO constants");
        assertThat(hpoConfiguration.getTimeLimit()).isEqualTo(600);
        assertThat(hpoConfiguration.getMaxExpWithSameBestScore()).isEqualTo(5);
    }

}