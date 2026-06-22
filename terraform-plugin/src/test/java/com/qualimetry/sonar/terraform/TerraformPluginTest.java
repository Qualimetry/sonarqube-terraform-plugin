package com.qualimetry.sonar.terraform;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin;

class TerraformPluginTest {

    @Test
    void isSonarPlugin() {
        TerraformPlugin plugin = new TerraformPlugin();
        assertThat(plugin).isInstanceOf(Plugin.class);
    }

    @Test
    void registersExpectedExtensionClasses() {
        assertThat(TerraformRulesDefinition.class).isNotNull();
        assertThat(TerraformQualityProfile.class).isNotNull();
        assertThat(TerraformSensor.class).isNotNull();
    }
}
