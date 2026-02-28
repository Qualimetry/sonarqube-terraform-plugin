package com.qualimetry.sonar.terraform;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonar.api.server.rule.RulesDefinition;

import com.qualimetry.terraform.rules.RuleRegistry;

class TerraformRulesDefinitionTest {

    @Test
    void repositoryHasExpectedRuleCount() {
        RuleRegistry registry = new RuleRegistry();
        assertThat(registry.getAllRules()).isNotEmpty();
    }

    @Test
    void repositoryKeyAndName() {
        assertThat(TerraformRulesDefinition.REPOSITORY_KEY).isEqualTo("qualimetry-terraform");
        assertThat(TerraformRulesDefinition.REPOSITORY_NAME).isEqualTo("Qualimetry Terraform");
        assertThat(TerraformRulesDefinition.LANGUAGE).isEqualTo("terraform");
    }
}
