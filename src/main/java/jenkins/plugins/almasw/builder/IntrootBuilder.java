package jenkins.plugins.almasw.builder;

import static hudson.util.jna.GNUCLibrary.LIBC;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Project;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import jenkins.plugins.almasw.builder.deps.IntrootDep;
import jenkins.plugins.almasw.builder.deps.IntrootDepResId;
import net.sf.json.JSONObject;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

public class IntrootBuilder extends Builder {
		
	public transient final int cores;

	public String acs;
	public final String module;
	public final boolean verbose;
	public final boolean pars;
	public final int jobs;
	public final int limit;
	public final boolean noStatic;
	public final boolean noIfr;
	public final List<IntrootDep> dependencies;
	public final boolean ccache;
	public final String introot;
	public final boolean dry;
	public Date date;
	
	private transient ArrayList<String> intlist;
	private transient String makePars;
	
	@DataBoundConstructor
	public IntrootBuilder(String acs, String module, boolean verbose,
			boolean pars, int jobs, int limit, boolean noStatic, boolean noIfr,
			List<IntrootDep> dependencies, boolean ccache, String introot, boolean dry) {

		this.cores = Runtime.getRuntime().availableProcessors();
		
		this.acs = acs;
		this.module = module;
		this.verbose = verbose;
		this.pars = pars;
		this.jobs = jobs;
		this.limit = limit;
		this.noStatic = noStatic;
		this.noIfr = noIfr;
		this.dependencies = dependencies;
		this.ccache = ccache;
		this.introot = introot;
		this.dry = dry;
	}
	
	public String getCachedMakePars() {
		if(this.makePars == null)
			this.makePars = this.getMakePars();
		return this.makePars;
	}
	
	public String getMakePars() {
		
		StringBuilder makePars = new StringBuilder();
		
		if(this.pars && this.cores > 1) {
			makePars.append("-j");
			
			if(this.jobs < 0) {
				makePars.append(this.cores - 1);
			} else if(this.jobs == 0){
				makePars.append(1);
			} else {
				makePars.append(this.jobs);
			}
			
			if( this.limit != 0 ) {
				makePars.append(" -l");
				if(this.limit < 0) {
					makePars.append(this.cores - 1);
				} else {
					makePars.append(this.limit);
				}
			}
		}
		
		return makePars.toString();
	}
	
	public ArrayList<String> getCachedIntlist() {
		
		if(this.intlist == null)
			this.intlist = this.getIntlist();
		
		return this.intlist;
	}
	
	// api: not used due builds, nor other stuff can be not still exists
	// at execution time?, instead using the jenkins hardcoded path..?
	// for the moment, 
	public ArrayList<String> getIntlist() {
		
		ArrayList<String> intlist = new ArrayList<String>();
		
		for(IntrootDep introot: this.getDependencies()) {
			for(Project project: Jenkins.getInstance().getProjects()) {
				if(introot.getProject().equalsIgnoreCase(project.getName())) {
					intlist.add(introot.getIntroot());
					break;
				}
			}
		}
		
		return intlist;
	}
	
	public File generateScript(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException {		
		VelocityEngine velocity = new VelocityEngine();

		velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		velocity.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		velocity.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.SimpleLog4JLogSystem");
		velocity.setProperty("runtime.log.logsystem.log4j.category", "velocity");
		velocity.setProperty("runtime.log.logsystem.log4j.logger", "velocity");
		velocity.setProperty("log4j.logger.org.apache.velocity.runtime.log.SimpleLog4JLogSystem", "INFO");
		velocity.init();
				
		Template template = velocity.getTemplate("template/almasw-builder.template");
		
		VelocityContext context = new VelocityContext();

		context.put("builder", this);
		context.put("build", build);
		context.put("launcher", launcher);
		context.put("listener", listener);
		
		StringWriter stringWriter = new StringWriter();
		template.merge(context, stringWriter);
		
		String script = this.module + "_" + build.getNumber();
		String workspace =  (String) build.getEnvVars().get("WORKSPACE");

		File scriptFile = new File(workspace, script);
		PrintWriter printWriter = new PrintWriter(scriptFile);
		printWriter.print(stringWriter.toString());
		printWriter.close();
		scriptFile.setExecutable(true);
		
		return scriptFile;	
	}
	
	public void generateInfo(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
		
		String nfo = this.module + "_" + build.getNumber() + ".nfo";
		String workspace =  (String) build.getEnvVars().get("WORKSPACE");
		
		File nfoFile = new File(workspace, nfo);
		PrintWriter printWriter = new PrintWriter(nfoFile);

		this.writeAndLog(printWriter, listener, "/start");
		
		this.writeAndLog(printWriter, listener, "/id=", String.valueOf(build.getNumber()));
		this.writeAndLog(printWriter, listener, "/module=", this.getModule());
		this.writeAndLog(printWriter, listener, "/acs =", this.getAcs());
		this.writeAndLog(printWriter, listener, "/noifrcheck=", String.valueOf(this.getNoIfr()));
		this.writeAndLog(printWriter, listener, "/nostatic=", String.valueOf(this.getNoStatic()));
		this.writeAndLog(printWriter, listener, "/verbose=", String.valueOf(this.getVerbose()));
		this.writeAndLog(printWriter, listener, "/dry=", String.valueOf(this.getDry()));
		this.writeAndLog(printWriter, listener, "/ccache=", String.valueOf(this.getCcache()));
		
		if(this.getPars()) {
			this.writeAndLog(printWriter, listener, "/makejobs=", String.valueOf(this.getMakePars()));	
		}

		if(this.getDependencies() != null && this.getDependencies().size() > 0) {
			this.writeAndLog(printWriter, listener, "/intlist=start");
			
			for(IntrootDep introot: this.getDependencies()) {
				this.writeAndLog(printWriter, listener, "/intlist/introot=start");	
				this.writeAndLog(printWriter, listener, "/intlist/introot/project=" + introot.getProject());
				this.writeAndLog(printWriter, listener, "/intlist/introot/path=", introot.getIntroot());
				if(introot.getIsArtifact()) {
					String id = introot.getResult().getJenkinsId();
					String artifactPath = introot.getProjectRootArtifact(id).toString();
					String artifactRealPath = artifactPath.replace("$JENKINS_HOME", build.getEnvVars().get("JENKINS_HOME").toString());
					FilePath symlink = new FilePath(new File(artifactRealPath));
					this.writeAndLog(printWriter, listener, "/intlist/introot/artifact/source=", introot.getResult().getJenkinsId());
					this.writeAndLog(printWriter, listener, "/intlist/introot/artifact/path=", introot.getACSSW(), symlink.readLink());
				} else {
					this.writeAndLog(printWriter, listener, "/intlist/introot/workspace=", introot.getACSSW());
				}
				this.writeAndLog(printWriter, listener, "/intlist/introot=end");
			}
			this.writeAndLog(printWriter, listener, "/intlist=end");
		}
		
		this.writeAndLog(printWriter, listener, "/end");
		printWriter.close();
	}
	
	public void getCanonicalArtifact(IntrootDep introot) {
		StringBuilder introotPath = new StringBuilder();
		introotPath.append("$JENKINS_HOME");
		introotPath.append(File.separator);
		introotPath.append("jobs");
		introotPath.append(File.separator);
		introotPath.append(introot.getProject());
		introotPath.append(introot.getResult().getJenkinsId());
	}
	
	public void writeAndLog(PrintWriter printWriter, BuildListener listener, String ... words) {
		this.log(listener, words);
		this.write(printWriter, words);
	}
	
	public void write(PrintWriter printWriter, String ... words) {
		StringBuilder builder = new StringBuilder(RuntimeConfiguration.LOGGER_PREFIX);
		for(String word: words)
			builder.append(word);
		printWriter.println(builder.toString());
	}
	
	public void log(BuildListener listener, String ... words) {
		StringBuilder builder = new StringBuilder(RuntimeConfiguration.LOGGER_PREFIX);
		for(String word: words)
			builder.append(word);
		listener.getLogger().println(builder.toString());
	}
	
	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		PrintStream logger = listener.getLogger();
		String workspace =  (String) build.getEnvVars().get("WORKSPACE");

		this.generateInfo(build, listener);
		File script = this.generateScript(build, launcher, listener);
		
		StringBuilder command =  new StringBuilder();
		command.append("sh -x ");
		
		if(this.dry) {
			command.append("-n ");
		}
		
		command.append(script.getName());
				
		try {
			ProcStarter process = launcher
					.launch()
					.envs(build.getEnvVars())
					.pwd(workspace)
					.cmdAsSingleString(command.toString())
					.stdout(logger)
					.stderr(logger);
			process.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}	
		return true;
	}

	public boolean hasIntlist() {
		return this.getDependencies() != null && this.getDependencies().size() > 0;
	}
	
	@Exported
 	public List<IntrootDep> getDependencies() {
		return dependencies;
	}
	
	@Exported
	public String getModule() {
		return module;
	}

	@Exported
	public boolean getVerbose() {
		return verbose;
	}
	
	@Exported
	public boolean getPars() {
		return pars;
	}

	@Exported
	public int getCores() {
		return cores;
	}

	@Exported
	public int getJobs() {
		return jobs;
	}

	@Exported
	public int getLimit() {
		return limit;
	}

	@Exported
	public boolean getNoStatic() {
		return noStatic;
	}

	@Exported
	public boolean getNoIfr() {
		return noIfr;
	}

	@Exported
	public String getAcs() {
		return acs;
	}
	
	@Exported
	public boolean getCcache() {
		return ccache;
	}
	
	@Exported
	public String getIntroot() {
		return introot;
	}
	
	@Exported
	public boolean getDry() {
		return dry;
	}

	@Override
	public IntrootBuilderDescriptor getDescriptor() {
		return (IntrootBuilderDescriptor) super.getDescriptor();
	}
	
	@Extension
	public static final class IntrootBuilderDescriptor extends BuildStepDescriptor<Builder> {

		private String ccacheInstall;
		private String acsInstall;
		
		public IntrootBuilderDescriptor() {
			load();
		}
		
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}
		
		public String getDisplayName() {
			return "ALMA Software Module Builder";
		}
		
		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			ccacheInstall = formData.getString("ccacheInstall");
			acsInstall = formData.getString("acsInstall");
			save();
			return super.configure(req, formData);
		}

		public FormValidation doCheckName(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please set a name");
			if (value.length() < 4)
				return FormValidation.warning("Isn't the name too short?");
			return FormValidation.ok();
		}
		
		public ListBoxModel doFillAcsItems() {
			
			String basePath = (this.acsInstall != null && !this.acsInstall.isEmpty()) ? this.acsInstall : "/alma"; 
			File almaInstall = new File(basePath);
			
			ListBoxModel items = new ListBoxModel();
			Set<String> installations = new HashSet<String>();
			
			items.add(basePath + File.separator + "ACS-current");
			
			try {
				for(File installation : almaInstall.listFiles(
						new FileFilter() {
							public boolean accept(File pathname) { 
								return pathname.isDirectory(); 
							}
				})) {
					if(installations.add(installation.getCanonicalPath()))
						items.add(installation.getCanonicalPath());
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}	
			
			installations.clear();
			return items;
		}

		public String getCcacheInstall() {
			return ccacheInstall;
		}
		
		public String getAcsInstall() {
			return acsInstall;
		}
	}
}
