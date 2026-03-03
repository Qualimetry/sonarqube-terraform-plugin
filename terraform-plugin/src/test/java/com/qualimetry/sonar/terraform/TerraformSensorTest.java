package com.qualimetry.sonar.terraform;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;

class TerraformSensorTest {

    @Test
    void descriptorRunsWithoutThrowing() {
        TerraformSensor sensor = new TerraformSensor();
        SensorDescriptor descriptor = mock(SensorDescriptor.class);
        sensor.describe(descriptor);
    }

    @Test
    void executeSkipsWhenNoTerraformFiles(@TempDir File tempDir) {
        SensorContext context = mock(SensorContext.class);
        FileSystem fs = mock(FileSystem.class);
        FilePredicates predicates = mock(FilePredicates.class);
        when(context.fileSystem()).thenReturn(fs);
        when(fs.predicates()).thenReturn(predicates);
        when(fs.hasFiles(any())).thenReturn(false);
        when(fs.baseDir()).thenReturn(tempDir);

        TerraformSensor sensor = new TerraformSensor();
        sensor.execute(context);
    }

    @Test
    void executeRunsWhenTerraformFilesPresent(@TempDir File tempDir) throws Exception {
        File mainTf = new File(tempDir, "main.tf");
        Files.writeString(mainTf.toPath(), "variable \"x\" {}");
        SensorContext context = mock(SensorContext.class);
        FileSystem fs = mock(FileSystem.class);
        FilePredicates predicates = mock(FilePredicates.class);
        when(context.fileSystem()).thenReturn(fs);
        when(fs.predicates()).thenReturn(predicates);
        when(fs.hasFiles(any())).thenReturn(true);
        when(fs.baseDir()).thenReturn(tempDir);
        when(fs.inputFile(any())).thenReturn(null);

        TerraformSensor sensor = new TerraformSensor();
        sensor.execute(context);
    }
}
