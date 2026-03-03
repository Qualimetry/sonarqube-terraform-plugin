package com.qualimetry.sonar.terraform;

import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

import com.qualimetry.terraform.rules.DescriptionLoader;
import com.qualimetry.terraform.rules.RuleDescriptionBuilder;
import com.qualimetry.terraform.rules.RuleRegistry;

/**
 * Defines the Qualimetry Terraform rule repository: one rule per entry in rules.json
 * with HTML description, severity, and type. Uses SonarQube's existing "terraform" language.
 */
public class TerraformRulesDefinition implements RulesDefinition {

    public static final String REPOSITORY_KEY = RuleRegistry.REPOSITORY_KEY;
    public static final String REPOSITORY_NAME = "Qualimetry Terraform";
    public static final String LANGUAGE = "terraform";

    private final RuleRegistry registry = new RuleRegistry();

    @Override
    public void define(Context context) {
        NewRepository repo = context.createRepository(REPOSITORY_KEY, LANGUAGE).setName(REPOSITORY_NAME);
        for (com.qualimetry.terraform.rules.Rule r : registry.getAllRules()) {
            NewRule newRule = repo.createRule(r.getId())
                    .setName(r.getName())
                    .setSeverity(r.getSeverity())
                    .setType(RuleType.valueOf(r.getType()));
            String html = DescriptionLoader.loadHtml(r.getId());
            if ((html == null || html.isEmpty()) && r.getDescriptionSummary() != null) {
                html = RuleDescriptionBuilder.buildHtml(r);
            }
            if (html != null && !html.isEmpty()) {
                newRule.setHtmlDescription(html);
            }
            if (r.getTags() != null && !r.getTags().isEmpty()) {
                newRule.setTags(r.getTags().toArray(new String[0]));
            } else if (r.getDocUrl() != null && !r.getDocUrl().isEmpty()) {
                newRule.setTags("terraform", "external-info");
            }
        }
        repo.done();
    }
}
