package com.cstestforge.codegen.service.generator;

import com.cstestforge.codegen.model.event.*;
import com.cstestforge.codegen.service.template.TemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract base class for code generators
 */
public abstract class AbstractCodeGenerator implements CodeGenerator {
    
    protected final TemplateEngine templateEngine;
    protected final String frameworkId;
    protected final String language;
    protected final String indentString;
    
    public AbstractCodeGenerator(TemplateEngine templateEngine, String frameworkId, String language) {
        this.templateEngine = templateEngine;
        this.frameworkId = frameworkId;
        this.language = language;
        this.indentString = "    "; // 4 spaces by default
    }
    
    @Override
    public String getFrameworkId() {
        return frameworkId;
    }
    
    @Override
    public String getLanguage() {
        return language;
    }
    
    @Override
    public String generateCode(List<Event> events) {
        StringBuilder code = new StringBuilder();
        
        // Generate imports and setup
        code.append(generateImports());
        code.append("\n\n");
        code.append(generateClassHeader());
        code.append("\n\n");
        
        // Generate code for each event
        for (Event event : events) {
            code.append(generateEventCode(event, 1));
            code.append("\n");
        }
        
        // Generate class footer
        code.append(generateClassFooter());
        
        return code.toString();
    }
    
    /**
     * Generate code for a specific event with indentation
     * 
     * @param event the event to generate code for
     * @param indentLevel the level of indentation
     * @return the generated code
     */
    protected String generateEventCode(Event event, int indentLevel) {
        if (event instanceof ConditionalEvent) {
            return generateConditional((ConditionalEvent) event, indentLevel);
        } else if (event instanceof LoopEvent) {
            return generateLoop((LoopEvent) event, indentLevel);
        } else if (event instanceof CaptureEvent) {
            return generateCapture((CaptureEvent) event, indentLevel);
        } else if (event instanceof AssertionEvent) {
            return generateAssertion((AssertionEvent) event, indentLevel);
        } else if (event instanceof GroupEvent) {
            return generateGroup((GroupEvent) event, indentLevel);
        } else if (event instanceof TryCatchEvent) {
            return generateTryCatch((TryCatchEvent) event, indentLevel);
        } else if (event instanceof ActionEvent) {
            return generateAction((ActionEvent) event, indentLevel);
        } else {
            return indent(indentLevel) + "// Unsupported event type: " + event.getType();
        }
    }
    
    /**
     * Generate imports for the code
     * 
     * @return the generated imports
     */
    protected abstract String generateImports();
    
    /**
     * Generate the class header
     * 
     * @return the generated class header
     */
    protected abstract String generateClassHeader();
    
    /**
     * Generate the class footer
     * 
     * @return the generated class footer
     */
    protected abstract String generateClassFooter();
    
    /**
     * Generate indentation string for the given level
     * 
     * @param level the indentation level
     * @return the indentation string
     */
    protected String indent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append(indentString);
        }
        return sb.toString();
    }
    
    /**
     * Generate code for a list of events with indentation
     * 
     * @param events the events to generate code for
     * @param indentLevel the level of indentation
     * @return the generated code
     */
    protected String generateEventsCode(List<Event> events, int indentLevel) {
        if (events == null || events.isEmpty()) {
            return "";
        }
        
        return events.stream()
                .map(event -> generateEventCode(event, indentLevel))
                .collect(Collectors.joining("\n"));
    }
    
    /**
     * Create a context map for template rendering
     * 
     * @return the context map
     */
    protected Map<String, Object> createContext() {
        return new HashMap<>();
    }
} 