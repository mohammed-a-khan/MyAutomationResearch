package com.cstestforge.codegen.service.generator;

import com.cstestforge.codegen.model.event.*;
import java.util.List;

/**
 * Interface for code generators that convert events to code in specific languages and frameworks
 */
public interface CodeGenerator {
    /**
     * Generate code from a list of events
     * 
     * @param events the events to generate code from
     * @return the generated code as a string
     */
    String generateCode(List<Event> events);
    
    /**
     * Generate code for a conditional event
     * 
     * @param event the conditional event
     * @param indentLevel the level of indentation
     * @return the generated code as a string
     */
    String generateConditional(ConditionalEvent event, int indentLevel);
    
    /**
     * Generate code for a loop event
     * 
     * @param event the loop event
     * @param indentLevel the level of indentation
     * @return the generated code as a string
     */
    String generateLoop(LoopEvent event, int indentLevel);
    
    /**
     * Generate code for a capture event
     * 
     * @param event the capture event
     * @param indentLevel the level of indentation
     * @return the generated code as a string
     */
    String generateCapture(CaptureEvent event, int indentLevel);
    
    /**
     * Generate code for an assertion event
     * 
     * @param event the assertion event
     * @param indentLevel the level of indentation
     * @return the generated code as a string
     */
    String generateAssertion(AssertionEvent event, int indentLevel);
    
    /**
     * Generate code for a group event
     * 
     * @param event the group event
     * @param indentLevel the level of indentation
     * @return the generated code as a string
     */
    String generateGroup(GroupEvent event, int indentLevel);
    
    /**
     * Generate code for a try-catch event
     * 
     * @param event the try-catch event
     * @param indentLevel the level of indentation
     * @return the generated code as a string
     */
    String generateTryCatch(TryCatchEvent event, int indentLevel);
    
    /**
     * Generate code for an action event
     * 
     * @param event the action event
     * @param indentLevel the level of indentation
     * @return the generated code as a string
     */
    String generateAction(ActionEvent event, int indentLevel);
    
    /**
     * Get the framework ID that this generator handles
     * 
     * @return the framework ID
     */
    String getFrameworkId();
    
    /**
     * Get the language that this generator handles
     * 
     * @return the language
     */
    String getLanguage();
} 