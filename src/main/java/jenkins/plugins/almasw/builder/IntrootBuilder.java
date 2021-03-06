package jenkins.plugins.almasw.builder;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import jenkins.plugins.almasw.builder.deps.IntrootDep;
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

/**
 * 
 * @author atejeda
 *
 */
public class IntrootBuilder extends Builder {
		
	public transient final int cores;

	public String acs;
	public final String module;
	public final boolean verbose;
	public final boolean pars;
	public final String jobs;
	public final String limit;
	public final boolean noStatic;
	public final boolean noIfr;
	public final List<IntrootDep> dependencies;
	public final boolean ccache;
	public final String introot;
	public final boolean dry;
	public final boolean trace;
	public Date date;
	
	private transient ArrayList<String> intlist;
	private transient String makePars;
	
	/**
	 * 
	 * @param acs
	 * @param module
	 * @param verbose
	 * @param pars
	 * @param jobs
	 * @param limit
	 * @param noStatic
	 * @param noIfr
	 * @param dependencies
	 * @param ccache
	 * @param introot
	 * @param dry
	 * @param trace
	 */
	@DataBoundConstructor
	public IntrootBuilder(String acs, String module, boolean verbose,
			boolean pars, String jobs, String limit, boolean noStatic, boolean noIfr,
			List<IntrootDep> dependencies, boolean ccache, String introot, boolean dry, boolean trace) {

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
		this.trace = trace;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getCachedMakePars() {
		if(this.makePars == null)
			this.makePars = this.getMakePars();
		return this.getMakePars();
	}
	
	/**
	 * 
	 * @return
	 */
	public String getMakePars() {
		
		StringBuilder makePars = new StringBuilder();
		
		if(this.getCores() > 1) {
			
			makePars.append("-j");
			
			if(this.getJobs() == null || this.getJobs().isEmpty() || Integer.valueOf(this.jobs) < 0) {
				makePars.append(this.getCores() - 1);
			} else if(Integer.valueOf(this.getJobs()) == 0){
				makePars.append(1);
			} else {
				makePars.append(this.getJobs());
			}
			
			if(this.getLimit() != null && !this.getLimit().isEmpty() && Integer.valueOf(this.getLimit()) != 0 ) {
				makePars.append(" -l");
				if(Integer.valueOf(this.limit) < 0) {
					makePars.append(this.cores - 1);
				} else {
					makePars.append(this.limit);
				}
			}
		}
		
		return makePars.toString();
	}
	
	/**
	 * 
	 * @return
	 */
	public ArrayList<String> getCachedIntlist() {
		
		if(this.intlist == null)
			this.intlist = this.getIntlist();
		
		return this.intlist;
	}
	
	// api: not used due builds, nor other stuff can be not still exists
	// at execution time?, instead using the jenkins hardcoded path..?
	// for the moment, 
	/**
	 * 
	 * @return
	 */
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
	
	/**
	 * 
	 * @param build
	 * @param launcher
	 * @param listener
	 * @return
	 * @throws IOException
	 */
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
	
	/**
	 * 
	 * @param build
	 * @param listener
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void generateInfo(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
		
		String nfo = this.module + "_" + build.getNumber() + ".nfo";
		String workspace =  (String) build.getEnvVars().get("WORKSPACE");
		
		File nfoFile = new File(workspace, nfo);
		PrintWriter printWriter = new PrintWriter(nfoFile);

		this.log(listener, "/start");
		
		this.log(listener, "/id=", String.valueOf(build.getNumber()));
		this.log(listener, "/cores=", String.valueOf(this.getCores()));
		this.log(listener, "/module=", this.getModule());
		this.log(listener, "/acs=", this.getAcs());
		this.log(listener, "/noifrcheck=", String.valueOf(this.getNoIfr()));
		this.log(listener, "/nostatic=", String.valueOf(this.getNoStatic()));
		this.log(listener, "/verbose=", String.valueOf(this.getVerbose()));
		this.log(listener, "/dry=", String.valueOf(this.getDry()));
		this.log(listener, "/ccache=", String.valueOf(this.getCcache()));
		
		if(this.getPars()) {
			this.log(listener, "/makejobs=", String.valueOf(this.getMakePars()));	
		}

		if(this.getDependencies() != null && this.getDependencies().size() > 0) {
			this.log(listener, "/intlist=start");
			
			for(IntrootDep introot: this.getDependencies()) {
				this.log(listener, "/intlist/introot=start");	
				this.log(listener, "/intlist/introot/project=" + introot.getProject());
				this.log(listener, "/intlist/introot/path=", introot.getIntroot());
				if(introot.getIsArtifact()) {
					String id = introot.getResult().getJenkinsId();
					String artifactPath = introot.getBuildRootId(id).toString();
					String artifactRealPath = artifactPath.replace("$JENKINS_HOME", build.getEnvVars().get("JENKINS_HOME").toString());
					FilePath symlink = new FilePath(new File(artifactRealPath));
					this.log(listener, "/intlist/introot/artifact/source=", introot.getResult().getJenkinsId());
					this.log(listener, "/intlist/introot/artifact/path=", symlink.readLink(), File.separator, introot.getACSSW());
				} else {
					this.log(listener, "/intlist/introot/workspace=", introot.getACSSW());
				}
				this.log(listener, "/intlist/introot=end");
			}
			this.log(listener, "/intlist=end");
		}
		
		this.log(listener, "/end");
		printWriter.close();
	}
	
	/**
	 * 
	 * @param introot
	 */
	public void getCanonicalArtifact(IntrootDep introot) {
		StringBuilder introotPath = new StringBuilder();
		introotPath.append("$JENKINS_HOME");
		introotPath.append(File.separator);
		introotPath.append("jobs");
		introotPath.append(File.separator);
		introotPath.append(introot.getProject());
		introotPath.append(introot.getResult().getJenkinsId());
	}
	
	/**
	 * 
	 * @param listener
	 * @param words
	 */
	public void log(BuildListener listener, String ... words) {
		StringBuilder builder = new StringBuilder(RuntimeConfiguration.LOGGER_PREFIX);
		for(String word: words)
			builder.append(word);
		listener.getLogger().println(builder.toString());
	}
	
	/**
	 * 
	 * @param build
	 * @return
	 * @throws IOException
	 */
	public boolean hasErrors(AbstractBuild build, BuildListener listener) throws IOException {
		String workspace =  (String) build.getEnvVars().get("WORKSPACE");
		String module = new File(workspace, this.getModule()).getCanonicalPath();
		
		File buildLog = new File(module, "build.log");
		File buildlinuxLog = new File(module, "buildLinux.log");
		
		if(buildLog.exists()) {
			return this.hasErrorsLog(buildLog, listener);
		} else {
		    this.log(listener, " ", "no build.log file to check");
		}
		
		if(buildlinuxLog.exists()) {
			return this.hasErrorsLog(buildlinuxLog, listener);
		} else {
		    this.log(listener, " ", "no buildLinux.log file to check");
		}
		
		this.log(listener, " ", "lo build logs files to check, no errors found, this might be not true due ACS makefile return always 0");
		
		return false;
	}
	
	/**
	 * 
	 * @param logFile
	 * @return
	 * @throws IOException
	 */
	public boolean hasErrorsLog(File logFile, BuildListener listener) throws IOException {
	    
	    this.log(listener, " ", "checking ", logFile.getName(), " for errors");
		
		InputStream is = new FileInputStream(logFile); 
        InputStreamReader sr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(sr);
        
        for(String line = br.readLine(); br.readLine() != null; line = br.readLine()) {
        	for(String regex: RuntimeConfiguration.BUILD_ERRORS_REGEX) {
        		boolean match;
        		if(match = line.matches(regex)) {
        		    this.log(listener, " ", logFile.getName(), " error found: ", regex);
        			br.close();
            		return match;
            	}
        	}
        }

        br.close();
		return false;
	}
	
	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		PrintStream logger = listener.getLogger();
		String workspace =  (String) build.getEnvVars().get("WORKSPACE");

		this.generateInfo(build, listener);
		File script = this.generateScript(build, launcher, listener);
		
		StringBuilder command =  new StringBuilder();
		command.append("sh ");
		
		if(this.getDry()) {
			command.append("-n ");
		}
		
		if(this.getTrace()) {
			command.append("-x ");
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
		
		boolean result =  !this.hasErrors(build, listener);
		
		if(!result) {
		    this.log(listener, " build has erros, check the logs");
		}
		
		return this.getDry() ? this.getDry() : result;
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
	public String getJobs() {
		return jobs;
	}

	@Exported
	public String getLimit() {
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
	
	@Exported
	public boolean getTrace() {
		return trace;
	}

	@Override
	public IntrootBuilderDescriptor getDescriptor() {
		return (IntrootBuilderDescriptor) super.getDescriptor();
	}
	
	/**
	 * 
	 * @author atejeda
	 *
	 */
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

		/**
		 * 
		 * @param value
		 * @return
		 * @throws IOException
		 * @throws ServletException
		 */
		public FormValidation doCheckName(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please set a name");
			if (value.length() < 4)
				return FormValidation.warning("Isn't the name too short?");
			return FormValidation.ok();
		}
		
		/**
		 * 
		 * @return
		 */
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
