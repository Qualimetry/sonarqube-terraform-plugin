package com.qualimetry.sonar.terraform;

import static org.assertj.core.api.Assertions.assertThat;

import com.qualimetry.terraform.rules.RuleRegistry;

import org.junit.jupiter.api.Test;

class TerraformQualityProfileTest {

    @Test
    void profileNames() {
        assertThat(TerraformQualityProfile.PROFILE_NAME).isEqualTo("Qualimetry Terraform");
        assertThat(TerraformQualityProfile.QUALIMETRY_WAY_NAME).isEqualTo("Qualimetry Way");
    }

    @Test
    void qualimetryTerraformProfileHasRules() {
        RuleRegistry registry = new RuleRegistry();
        assertThat(registry.getQualimetryTerraformRuleKeys()).isNotEmpty();
    }

    @Test
    void qualimetryWayProfileHasAllRules() {
        RuleRegistry registry = new RuleRegistry();
        assertThat(registry.getQualimetryWayRuleKeys()).isNotEmpty();
        assertThat(registry.getQualimetryWayRuleKeys())
                .containsAll(registry.getQualimetryTerraformRuleKeys());
    }
}
