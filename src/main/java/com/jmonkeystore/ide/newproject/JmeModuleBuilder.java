package com.jmonkeystore.ide.newproject;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.projectView.actions.MarkRootActionBase;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalModuleBuilder;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.SystemProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.plugins.gradle.frameworkSupport.BuildScriptDataBuilder;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JmeModuleBuilder extends AbstractExternalModuleBuilder<GradleProjectSettings> {

    private final String TEMPLATE_GRADLE_SETTINGS = "Gradle Settings.gradle";
    private final String TEMPLATE_BUILD_GRADLE = "JmeGradleBuildScript.gradle";
    private final String TEMPLATE_MAIN_CLASS = "JmeMainClass.java";

    private final String TEMPLATE_ATTRIBUTE_PROJECT_NAME = "PROJECT_NAME";
    private final String TEMPLATE_ATTRIBUTE_MODULE_PATH = "MODULE_PATH";
    private final String TEMPLATE_ATTRIBUTE_MODULE_NAME = "MODULE_NAME";
    private final String TEMPLATE_ATTRIBUTE_JAVA_VERSION = "JAVA_VERSION";

    // java-related
    private final String PROJECT_GROUP_ID = "GROUP_ID";
    private final String PROJECT_ARTIFACT_ID = "ARTIFACT_ID";
    private final String PROJECT_VERSION = "VERSION";

    private final String JME_ENGINE_VERSION = "JME_ENGINE_VERSION";
    private final String JME_LWJGL_VERSION = "JME_LWJGL_VERSION";
    private final String JME_DEP_EFFECTS = "JME_DEP_EFFECTS";
    private final String JME_DEP_BULLET = "JME_DEP_BULLET";
    private final String JME_BULLET_TYPE = "JME_BULLET_TYPE";
    private final String JME_DEP_OGG = "JME_DEP_OGG";
    private final String JME_DEP_PLUGINS = "JME_DEP_PLUGINS";

    private ProjectSettings projectSettings = new ProjectSettings();

    private String javaVersion;

    public JmeModuleBuilder() {
        super(new ProjectSystemId(JmeModuleType.ID), new GradleProjectSettings());
    }

    public ProjectSettings getProjectSettings() {
        return projectSettings;
    }

    @Override
    public ModuleType getModuleType() {
        return StdModuleTypes.JAVA;
    }

    @Override
    public void setupRootModel(@NotNull ModifiableRootModel modifiableRootModel) {

        String contentEntryPath = getContentEntryPath();

        if (contentEntryPath == null) {
            return;
        }

        File contentRootDir = new File(getContentEntryPath());
        FileUtilRt.createDirectory(contentRootDir);

        LocalFileSystem fileSystem = LocalFileSystem.getInstance();
        VirtualFile modelContentRootDir = fileSystem.refreshAndFindFileByIoFile(contentRootDir);

        if (modelContentRootDir == null) {
            return;
        }

        modifiableRootModel.addContentEntry(modelContentRootDir);

        if (myJdk == null) {
            modifiableRootModel.inheritSdk();
        }
        else {
            modifiableRootModel.setSdk(myJdk);
        }

        javaVersion = modifiableRootModel.getSdk().getName();

        Project project = modifiableRootModel.getProject();
        String rootProjectPath = FileUtil.toCanonicalPath(project.getBasePath());

        // create the build.gradle file
        VirtualFile gradleBuildFile = setupGradleBuildFile(modelContentRootDir);

        // create the settings.gradle file
        setupGradleSettingsFile(
                rootProjectPath, modelContentRootDir, modifiableRootModel.getProject().getName(),
                modifiableRootModel.getModule().getName(), true);

        // create the sources and resources directories
        VirtualFile soureRootPath = setupSourceDirectory(modifiableRootModel, modelContentRootDir);
        setupResourcesDirectories(modifiableRootModel, modelContentRootDir);

        createMainClass(soureRootPath);
        // setupRunConfigurations(modelContentRootDir);
        // setupGitignore(modelContentRootDir);

        if (gradleBuildFile != null) {
            modifiableRootModel.getModule().putUserData(
                    Key.create("gradle.module.buildScriptData"),
                    new BuildScriptDataBuilder(gradleBuildFile));
        }

        // doesn't work.
        // openMainClass(modifiableRootModel.getProject(), soureRootPath);
    }

    @Nullable
    @Override
    public ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
        return new JmeModuleWizardStep(context, this);
    }

    private void openMainClass(Project project, VirtualFile sourcesRootPath) {
        VirtualFile mainClass = getOrCreateExternalProjectConfigFile(sourcesRootPath.getPath(), "Main.java");
        mainClass.refresh(false, false);
        new OpenFileDescriptor(project, mainClass).navigate(true);
    }

    private void createMainClass(VirtualFile sourcesRootPath) {

        String packageName = projectSettings.getGroupId() + "." + projectSettings.getArtifactId();

        Map<String, String> attributes = new HashMap<>();
        attributes.put("PACKAGE", packageName);

        VirtualFile mainClassFile = getOrCreateExternalProjectConfigFile(sourcesRootPath.getPath(), "Main.java");
        saveFile(mainClassFile, TEMPLATE_MAIN_CLASS, attributes);

    }

    private VirtualFile setupSourceDirectory(ModifiableRootModel modifiableRootModel, VirtualFile modelContentRootDir) {
        // String resourceRootPath = "${modelContentRootDir.path}/src/main/$language";
        String sourceRootPath = modelContentRootDir.getPath() + "/src/main/java";

        if (getContentEntryPath() == null) {
            return null;
        }

        VirtualFile contentRoot = LocalFileSystem.getInstance().findFileByPath(getContentEntryPath());

        if (contentRoot != null) {
            ContentEntry contentEntry = MarkRootActionBase.findContentEntry(modifiableRootModel, contentRoot);

            if (contentEntry != null) {
                contentEntry.addSourceFolder(VfsUtilCore.pathToUrl(sourceRootPath), JavaSourceRootType.SOURCE);
            }
        }

        try {

            String dir = sourceRootPath
                    + "/"
                    + projectSettings.getGroupId().replace(".", "/")
                    + "/"
                    + projectSettings.getArtifactId().replace(".", "/");

            return VfsUtil.createDirectories(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void setupResourcesDirectories(ModifiableRootModel modifiableRootModel, VirtualFile modelContentRootDir) {

        String resourcesRootPath = modelContentRootDir.getPath() + "/src/main/resources";

        if (getContentEntryPath() == null) {
            return;
        }

        VirtualFile contentRoot = LocalFileSystem.getInstance().findFileByPath(getContentEntryPath());

        if (contentRoot != null) {
            ContentEntry contentEntry = MarkRootActionBase.findContentEntry(modifiableRootModel, contentRoot);

            if (contentEntry != null) {
                contentEntry.addSourceFolder(VfsUtilCore.pathToUrl(resourcesRootPath), JavaResourceRootType.RESOURCE);
            }
        }

        String[] directories = { "Interface", "MatDefs", "Materials", "Models", "Scenes", "Shaders", "Sounds", "Textures" };

        for (String resource : directories) {

            try {

                String dir = resourcesRootPath + "/" + resource;

                VfsUtil.createDirectories(dir);
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    private VirtualFile setupGradleSettingsFile(String rootProjectPath, VirtualFile modelContentRootDir, String projectName, String moduleName, boolean renderNewFile) {

        VirtualFile file = getOrCreateExternalProjectConfigFile(rootProjectPath, GradleConstants.SETTINGS_FILE_NAME);

        if (file == null) {
            return null;
        }

        String moduleDirName = VfsUtilCore.getRelativePath(modelContentRootDir, file.getParent(), '/');

        Map<String, String> attributes = new HashMap<>();

        attributes.put(TEMPLATE_ATTRIBUTE_PROJECT_NAME, projectName);
        attributes.put(TEMPLATE_ATTRIBUTE_MODULE_PATH, moduleDirName);
        attributes.put(TEMPLATE_ATTRIBUTE_MODULE_NAME, moduleName);

        saveFile(file, TEMPLATE_GRADLE_SETTINGS, attributes);

        return file;
    }

    private VirtualFile setupGradleBuildFile(VirtualFile modelContentRootDir) {
        VirtualFile file = getOrCreateExternalProjectConfigFile(modelContentRootDir.getPath(), GradleConstants.DEFAULT_SCRIPT_NAME);

        if (file == null) {
            return null;
        }

        Map<String, String> attributes = new HashMap<>();

        attributes.put(PROJECT_VERSION, projectSettings.getVersion());
        attributes.put(PROJECT_ARTIFACT_ID, projectSettings.getArtifactId());
        attributes.put(PROJECT_GROUP_ID, projectSettings.getGroupId());

        attributes.put(JME_ENGINE_VERSION, projectSettings.getEngineVersion());
        attributes.put(JME_LWJGL_VERSION, projectSettings.getLwjglVersion());
        attributes.put(JME_DEP_EFFECTS, "" + projectSettings.isUseEffectsDependency());
        attributes.put(JME_DEP_BULLET, "" + projectSettings.isUseBulletPhysicsDependency());
        attributes.put(JME_BULLET_TYPE, projectSettings.getBulletPhysicsDependencyType());
        attributes.put(JME_DEP_OGG, "" + projectSettings.isUseOggDependency());
        attributes.put(JME_DEP_PLUGINS, "" + projectSettings.isUsePluginsDependency());

        attributes.put(TEMPLATE_ATTRIBUTE_JAVA_VERSION, javaVersion);

        saveFile(file, TEMPLATE_BUILD_GRADLE, attributes);

        return file;
    }

    private VirtualFile getOrCreateExternalProjectConfigFile(String parent, String fileName) {
        File file = new File(parent, fileName);
        FileUtilRt.createIfNotExists(file);
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
    }

    private void saveFile(VirtualFile file, String templateName, Map<String, String> templateAttributes) {
        FileTemplateManager manager = FileTemplateManager.getDefaultInstance();
        FileTemplate template = manager.getInternalTemplate(templateName);

        try {

            String text = (templateAttributes != null)
                    ? template.getText(templateAttributes)
                    : template.getText();

            appendToFile(file, text);
        } catch (IOException e) {
            // throw new ConfigurationException(e.getMessage(), e.getStackTrace().toString());
            e.printStackTrace();
        }
    }

    private void appendToFile(VirtualFile file, String text) throws IOException {
        String lineSeparator = LoadTextUtil.detectLineSeparator(file, true);

        if (lineSeparator == null) {
            // lineSeparator = CodeStyleSettingsManager.getSettings(ProjectManagerEx.getInstanceEx().getDefaultProject()).getLineSeparator();
            // CodeStyleSettingsManager.getInstance(ProjectManagerEx.getInstanceEx().getDefaultProject()).getMainProjectCodeStyle().getLineSeparator();
            lineSeparator = SystemProperties.getLineSeparator();
        }

        String existingText = VfsUtilCore.loadText(file).trim();

        String content = existingText.isEmpty()
                ? "" :
                (existingText + lineSeparator);

        content += StringUtil.convertLineSeparators(text, lineSeparator);

        VfsUtil.saveText(file, content);
    }



}
