package almasw.jenkinsci.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor.FormException;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;


public class ModuleBuilder extends Builder {
	
	private String module;
	private boolean verbose;
	private String jobs;
	private boolean noStatic;
	private boolean noIfr;
	
	private List<ModuleDependencies> dependencies = new ArrayList<ModuleDependencies>();
	
	// "DataBoundConstructor"
	@DataBoundConstructor
	public ModuleBuilder(String module, boolean verbose,
			String jobs, boolean noStatic, boolean noIfr, List<ModuleDependencies> dependencies) {

		this.module = module;
		this.verbose = verbose;
		this.jobs = jobs;
		this.noStatic = noStatic;
		this.noIfr = noIfr;
		this.dependencies = dependencies;
	}


	@Exported
	public String getModule() {
		return module;
	}

	
	public void setModule(String module) {
		this.module = module;
	}

	@Exported
	public boolean getVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	@Exported
	public String getJobs() {
		return jobs;
	}

	public void setJobs(String jobs) {
		this.jobs = jobs;
	}

	@Exported
	public boolean getNoStatic() {
		return noStatic;
	}

	public void setNoStatic(boolean noStatic) {
		this.noStatic = noStatic;
	}

	@Exported
	public boolean getNoIfr() {
		return noIfr;
	}

	public void setNoIfr(boolean noIfr) {
		this.noIfr = noIfr;
	}

	@Exported
	public List<ModuleDependencies> getDependencies() {
		return dependencies;
	}

	public void setDependencies(List<ModuleDependencies> dependencies) {
		this.dependencies = dependencies;
	}

	@Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
//         This is where you 'build' the project.
//         Since this is a dummy, we just say 'hello world' and call that a build.
//
//        // This also shows how you can consult the global configuration of the builder
//        if (getDescriptor().getUseFrench())
//            listener.getLogger().println("Bonjour, "+name+"!");
//        else
//            listener.getLogger().println("Hello, "+name+"!");
		
        return true;
    }
	
	// Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link ModuleBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private boolean useCcache;

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a name");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "ALMA Software Module Builder";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
        	useCcache = formData.getBoolean("useCcache");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         *
         * The method name is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         */
        public boolean getUseCcache() {
            return useCcache;
        }
    }
}
