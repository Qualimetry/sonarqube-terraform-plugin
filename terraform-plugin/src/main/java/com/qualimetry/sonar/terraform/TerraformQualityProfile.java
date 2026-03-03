package com.qualimetry.sonar.terraform;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import com.qualimetry.terraform.rules.RuleRegistry;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Built-in quality profiles for Terraform: Qualimetry Terraform (does not duplicate
 * Sonar Way Terraform rules), Qualimetry Way (recommended full set), and provider-specific
 * profiles (Qualimetry AWS Way, Qualimetry Azure Way).
 * Uses SonarQube's existing "terraform" language; no profile is set as default.
 */
public class TerraformQualityProfile implements BuiltInQualityProfilesDefinition {

    static final String PROFILE_NAME = "Qualimetry Terraform";
    static final String QUALIMETRY_WAY_NAME = "Qualimetry Way";
    static final String QUALIMETRY_AWS_WAY_NAME = "Qualimetry AWS Way";
    static final String QUALIMETRY_AZURE_WAY_NAME = "Qualimetry Azure Way";

    @Override
    public void define(Context context) {
        RuleRegistry registry = new RuleRegistry();
        defineProfile(context, PROFILE_NAME, new LinkedHashSet<>(registry.getQualimetryTerraformRuleKeys()));
        defineProfile(context, QUALIMETRY_WAY_NAME, new LinkedHashSet<>(registry.getQualimetryWayRuleKeys()));
        defineProfile(context, QUALIMETRY_AWS_WAY_NAME, new LinkedHashSet<>(registry.getQualimetryAWSWayRuleKeys()));
        defineProfile(context, QUALIMETRY_AZURE_WAY_NAME, new LinkedHashSet<>(registry.getQualimetryAzureWayRuleKeys()));
    }

    private static void defineProfile(Context context, String name, Set<String> ruleKeys) {
        var profile = context.createBuiltInQualityProfile(name, TerraformRulesDefinition.LANGUAGE);
        profile.setDefault(false);
        for (String ruleKey : ruleKeys) {
            profile.activateRule(TerraformRulesDefinition.REPOSITORY_KEY, ruleKey);
        }
        profile.done();
    }
}
