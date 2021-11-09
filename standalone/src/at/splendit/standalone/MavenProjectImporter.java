package at.splendit.standalone;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenProjectImporter {

	private static final Logger logger = LoggerFactory.getLogger(Activator.class); 
	

	public List<IJavaProject> importProjects(File workspaceRoot, List<String> folders) throws InterruptedException, CoreException {

		String projects = String.join(";", folders);
		logger.debug("Importing maven projects {} into workspace {}", projects, workspaceRoot);
		List<MavenProjectInfo> projectInfos = findMavenProjects(workspaceRoot, folders);
		List<IProject> importedProjects = importMavenProjects(projectInfos);
		List<IJavaProject> importedJavaProjects = createJavaProjects(importedProjects);
		return importedJavaProjects;

	}

	private List<MavenProjectInfo> findMavenProjects(File workspaceRoot, List<String> folders)
			throws InterruptedException {
		logger.debug("Scanning maven projects");
		MavenModelManager modelManager = MavenPlugin.getMavenModelManager();
		LocalProjectScanner lps = new LocalProjectScanner(workspaceRoot, folders, false, modelManager);
		lps.run(new NullProgressMonitor());
		List<MavenProjectInfo> projects = lps.getProjects();
		logger.debug("Getting maven projcet info");
		return collectMavenProjectInfo(projects);
	}

	private List<MavenProjectInfo> collectMavenProjectInfo(Collection<MavenProjectInfo> input) {
		List<MavenProjectInfo> toRet = new ArrayList<>();
		for (MavenProjectInfo info : input) {
			toRet.add(info);
			toRet.addAll(collectMavenProjectInfo(info.getProjects()));
		}
		return toRet;
	}

	private List<IProject> importMavenProjects(List<MavenProjectInfo> projectInfos) throws CoreException {
		logger.debug("Importing maven projects");
		ProjectImportConfiguration projectImportConfig = new ProjectImportConfiguration();
		IProjectConfigurationManager projectConfigurationManager = MavenPlugin.getProjectConfigurationManager();
		NullProgressMonitor nullMonitor = new NullProgressMonitor();
		List<IMavenProjectImportResult> results = projectConfigurationManager.importProjects(projectInfos, projectImportConfig, nullMonitor);
		logger.debug("Imported maven project results:");
		for(IMavenProjectImportResult result : results) {
			MavenProjectInfo info = result.getMavenProjectInfo();
			logger.debug("Imported project model: {}", info.getModel());
		}

		return results.stream()
			.map(IMavenProjectImportResult::getProject)
			.collect(Collectors.toList());
	}

	private List<IJavaProject> createJavaProjects(List<IProject> projects) throws CoreException {
		List<IJavaProject> javaProjects = new ArrayList<>();
		for (IProject project : projects) {
			if (!project.isOpen()) {
				logger.debug("The IProject {} is not opened. Opening IProject.", project.getName());
				project.open(new NullProgressMonitor());
			}
			doCreateJavaProject(project).ifPresent(javaProjects::add);
		}
		return javaProjects;
	}

	private Optional<IJavaProject> doCreateJavaProject(IProject project) throws CoreException {
		IProjectDescription description = project.getDescription();
		String[] natureIds = description.getNatureIds();
		String projectNatures = Arrays.stream(natureIds).collect(Collectors.joining(","));
		logger.debug("Project {} has nature ids: {}", description.getName(), projectNatures);
		
		if (project.hasNature(JavaCore.NATURE_ID)) {
			logger.debug("Creating single Java project {}", project.getName());
			IJavaProject javaProject = JavaCore.create(project);
			if (!javaProject.isOpen()) {
				logger.debug("The IJavaProject {} is not opened. Opening IJavaProject", project.getName());
				javaProject.open(new NullProgressMonitor());
			}
			return Optional.ofNullable(javaProject);
		} else {
			logger.debug("Skipping project {} without java nature", project.getName());
		}
		return Optional.empty();
	}

}
