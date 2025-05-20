# CSTestForge Playwright

A TypeScript/Playwright test automation framework that mirrors the architecture and capabilities of the Java/Selenium CSTestForge framework.

## Architecture

The framework follows a modular design with these key components:

```
src/main/typescript/cstestforge-playwright/
├── core/
│   ├── CSPlaywrightDriver.ts       # Browser control (WebDriver equivalent)
│   ├── CSBasePage.ts               # Base page class for all page objects
│   ├── CSBrowserManager.ts         # Singleton for managing browser instances
│   └── CSElement.ts                # Enhanced element handling
├── element/
│   └── CSElementInteractionHandler.ts  # Robust element interaction with retry logic
├── annotation/
│   └── CSFindBy.ts                 # Element locator decorators (FindBy equivalent)
├── ai/
│   └── CSSelfHealing.ts            # AI-powered element location with healing
├── reporting/
│   └── CSPlaywrightReporting.ts    # Custom test reporting
├── bdd/
│   ├── BDDRunner.ts                # BDD test execution engine
│   ├── BDDFactory.ts               # BDD component factory
│   ├── CSTestStep.ts               # Step definition decorator
│   └── ScenarioContext.ts          # Shared scenario state
├── utils/
│   ├── CSDataProvider.ts           # Test data management
│   ├── CSDataProviderProcessor.ts  # Data provider decoration
│   ├── CSMetaData.ts               # Test metadata annotation
│   ├── CSMetaDataProcessor.ts      # Metadata processing
│   └── CSTestRetry.ts              # Automatic test retry logic
├── features/
│   └── login.feature               # Example feature file
└── example_test/
    ├── page/
    │   └── LoginPage.ts            # Example page object
    ├── test/
    │   └── LoginTest.ts            # Example test
    └── bdd/
        └── steps/
            └── LoginSteps.ts       # Example BDD step implementations
```

## Key Features

1. **Robust Element Handling**: Smart retry mechanisms and self-healing element location
2. **Page Object Pattern**: Clean separation of test logic and page interactions
3. **BDD Support**: Cucumber-style Gherkin syntax for behavior-driven development
4. **Reporting**: Detailed HTML reporting with screenshots
5. **AI Capabilities**: Self-healing element location for more stable tests
6. **Test Data Management**: Multiple data sources for data-driven testing
7. **Automatic Retry**: Smart test retry based on failure types
8. **Test Metadata**: Annotation system for test categorization and filtering

## Getting Started

### Installation

```bash
npm install
```

### Running Tests

```bash
# Run all tests
npm test

# Run in headed mode
npm run test:headed

# Run with UI mode
npm run test:ui
```

### Creating a Page Object

```typescript
import { CSBasePage } from '../../core/CSBasePage';
import { CSFindById, CSFindByAI } from '../../annotation/CSFindBy';

export class LoginPage extends CSBasePage {
    protected readonly pageUrl: string = '/login';
    protected readonly pageTitle: string = 'Login Page';
    
    @CSFindById('username')
    private usernameInput!: Promise<Locator>;
    
    @CSFindById('password')
    private passwordInput!: Promise<Locator>;
    
    @CSFindById('login-button')
    private loginButton!: Promise<Locator>;
    
    // Methods...
}
```

### Writing a Test with Annotations

```typescript
import { test, expect } from '@playwright/test';
import { LoginPage } from '../page/LoginPage';
import { CSMetaData, Priority, Owner, Tags } from '../../utils/CSMetaData';
import { CSTestRetry } from '../../utils/CSTestRetry';

test.describe('Login functionality tests', () => {
    let loginPage: LoginPage;
    
    test.beforeEach(async () => {
        loginPage = new LoginPage();
    });
    
    @CSMetaData({
        description: 'Verify successful login with valid credentials',
        priority: 1
    })
    @CSTestRetry(2)
    test('Valid login should succeed', async () => {
        await loginPage.navigateTo();
        await loginPage.login('validUser', 'validPass');
        expect(await loginPage.isLoginSuccessful()).toBeTruthy();
    });
    
    @Priority(2)
    @Owner('TestTeam')
    @Tags('smoke', 'authentication')
    test('Invalid login should fail', async () => {
        await loginPage.navigateTo();
        await loginPage.login('invalidUser', 'invalidPass');
        expect(await loginPage.isLoginSuccessful()).toBeFalsy();
    });
});
```

### Data-Driven Testing

```typescript
import { test } from '@playwright/test';
import { LoginPage } from '../page/LoginPage';
import { CSDataProviderDecorator } from '../../utils/CSDataProviderProcessor';
import { SourceType } from '../../utils/CSDataProvider';

test.describe('Data-driven login tests', () => {
    @CSDataProviderDecorator({
        name: 'loginData',
        source: {
            type: SourceType.CSV,
            path: './data/login-data.csv'
        }
    })
    test('Login with various credentials', async (page, testInfo, username, password, expected) => {
        const loginPage = new LoginPage();
        await loginPage.navigateTo();
        await loginPage.login(username, password);
        await expect(loginPage.isLoginSuccessful()).toBe(expected === 'success');
    });
});
```

## BDD Testing

### Feature File

```gherkin
Feature: User Login
  
  Scenario: Successful login with valid credentials
    Given the user is on the login page
    When the user logs in with username "validUser" and password "validPass"
    Then the login should be successful
```

### Step Definition

```typescript
import { LoginPage } from '../../page/LoginPage';
import { CSBrowserManager } from '../../../core/CSBrowserManager';

export class LoginSteps {
    private loginPage: LoginPage;
    
    constructor() {
        this.loginPage = new LoginPage();
    }
    
    public async givenUserIsOnLoginPage(): Promise<void> {
        await this.loginPage.navigateTo();
    }
    
    // More steps...
}
```

## Configuration

Configuration is managed through the Playwright configuration file and environment variables.

## License

MIT 