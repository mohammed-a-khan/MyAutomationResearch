import { test, expect } from '@playwright/test';
import { csBdd } from '../test/CSBddRunner';
import { LoginSteps } from '../example_test/steps/LoginSteps';
import * as path from 'path';

/**
 * BDD-style test that executes the login.feature file
 * 
 * This test uses our CSBddRunner to parse and execute the Gherkin scenarios
 * defined in the feature file, using the step definitions in LoginSteps.
 */

// Create a context that will be passed to step methods
const stepsContext = {
    // Step definitions will be instantiated with the page
    createSteps: (page: any) => new LoginSteps(page),
};

// Configure the BDD runner
csBdd.feature(
    path.join(__dirname, 'features', 'login.feature'),
    stepsContext
);

// You can also configure global options for all features
/*
csBdd.loadAllFeatures({
    featuresDir: path.join(__dirname, 'features'),
    includeTags: ['smoke'], // Only run @smoke scenarios
    // excludeTags: ['slow'], // Skip @slow scenarios
    stopOnFailure: true,
    generateReports: true,
    beforeScenario: async (page, scenario) => {
        console.log(`Starting scenario: ${scenario}`);
    },
    afterScenario: async (page, scenario, success) => {
        console.log(`Finished scenario: ${scenario} - ${success ? 'Success' : 'Failed'}`);
    }
}, stepsContext);
*/ 