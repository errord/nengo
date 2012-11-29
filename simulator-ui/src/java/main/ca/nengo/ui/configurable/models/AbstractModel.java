/*
The contents of this file are subject to the Mozilla Public License Version 1.1 
(the "License"); you may not use this file except in compliance with the License. 
You may obtain a copy of the License at http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS" basis, WITHOUT
WARRANTY OF ANY KIND, either express or implied. See the License for the specific 
language governing rights and limitations under the License.

The Original Code is "AbstractConstructable.java". Description: 
"A UIModel which can be configured through the IConfigurable interface
  
  @author Shu Wu"

The Initial Developer of the Original Code is Bryan Tripp & Centre for Theoretical Neuroscience, University of Waterloo. Copyright (C) 2006-2008. All Rights Reserved.

Alternatively, the contents of this file may be used under the terms of the GNU 
Public License license (the GPL License), in which case the provisions of GPL 
License are applicable  instead of those above. If you wish to allow use of your 
version of this file only under the terms of the GPL License and not to allow 
others to use your version of this file under the MPL, indicate your decision 
by deleting the provisions above and replace  them with the notice and other 
provisions required by the GPL License.  If you do not delete the provisions above,
a recipient may use your version of this file under either the MPL or the GPL License.
*/

package ca.nengo.ui.configurable.models;

import java.util.Map;

import ca.nengo.ui.configurable.ConfigException;
import ca.nengo.ui.configurable.IConfigurable;
import ca.nengo.ui.configurable.Property;

/**
 * A UIModel which can be configured through the IConfigurable interface
 * 
 * @author Shu Wu
 */
public abstract class AbstractModel implements IConfigurable {

	private Object model;

	/**
	 * This function is called from a common thread, so it is not safe to put UI
	 * stuff here If there's UI Stuff to be done, put it in afterModelCreated
	 * Creates a model for the configuration process, called if a ConfigManager
	 * is used
	 * 
	 * @param configuredProperties The configured properties
	 */
	protected abstract Object configureModel(Map<Property, Object> configuredProperties)
			throws ConfigException;

	public void completeConfiguration(final Map<Property, Object> properties) throws ConfigException {
		model = null;
		model = configureModel(properties);
	}

	public String getDescription() {
		return getTypeName() + " constructor";
	}

	public Object getModel() {
		return model;
	}

	public abstract Property[] getSchema();

	public void preConfiguration(Map<Property, Object> props) throws ConfigException {
		// do nothing
	}
	
	public String getExtendedDescription() {
		return null;
	}
}