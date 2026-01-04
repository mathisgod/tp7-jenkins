import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "Features",
        // MODIFICATION ICI : On pointe vers le dossier build standard
        plugin = { "pretty", "json:build/reports/cucumber/report.json" }
)
public class ExampleTest {
}