package jenkins.plugins.almasw.builder.deps;

import java.io.Serializable;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

/**
 * 
 * @author atejeda
 *
 */
public abstract class IntrootDepRes implements Describable<IntrootDepRes>, ExtensionPoint, Serializable {
	
    /**
     * 
     * @return
     */
	public abstract String getJenkinsId();
	
	/**
	 * 
	 * @return
	 */
	public static DescriptorExtensionList<IntrootDepRes, IntrootDepResDescriptor> getDescriptors() {
		return Jenkins.getInstance().<IntrootDepRes, IntrootDepResDescriptor>getDescriptorList(IntrootDepRes.class);
	}
	
	/**
	 * 
	 */
	public IntrootDepResDescriptor getDescriptor() {
        return (IntrootDepResDescriptor)Jenkins.getInstance().getDescriptor(getClass());
    }

	/**
	 * 
	 * @author atejeda
	 *
	 */
	public static abstract class IntrootDepResDescriptor extends Descriptor<IntrootDepRes> {
		
        protected IntrootDepResDescriptor() { }

        protected IntrootDepResDescriptor(Class<? extends IntrootDepRes> clazz) {
            super(clazz);
        }
	}
}
