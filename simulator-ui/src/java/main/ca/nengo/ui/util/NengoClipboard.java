/*
The contents of this file are subject to the Mozilla Public License Version 1.1 
(the "License"); you may not use this file except in compliance with the License. 
You may obtain a copy of the License at http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS" basis, WITHOUT
WARRANTY OF ANY KIND, either express or implied. See the License for the specific 
language governing rights and limitations under the License.

The Original Code is "NengoClipboard.java". Description: 
""

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

package ca.nengo.ui.util;

import java.awt.geom.Point2D;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.LinkedList;

import ca.nengo.model.Node;

public class NengoClipboard {

	private ArrayList<Node> selectedObjs = null;
	private ArrayList<Point2D> objectOffsets = null;

	public static interface ClipboardListener {
		public void clipboardChanged();
	}

	LinkedList<ClipboardListener> listeners = new LinkedList<ClipboardListener>();

	public void addClipboardListener(ClipboardListener listener) {
		listeners.add(listener);
	}

	public void removeClipboardListener(ClipboardListener listener) {
		if (!listeners.contains(listener)) {
			listeners.remove(listener);
		} else {
			throw new InvalidParameterException();
		}

	}

	public void setContents(ArrayList<Node> nodes, ArrayList<Point2D> objOffsets) {
		selectedObjs = nodes;
		objectOffsets = objOffsets;
		fireChanged();
	}

	private void fireChanged() {
		for (ClipboardListener listener : listeners) {
			listener.clipboardChanged();
		}
	}

	public ArrayList<Node> getContents() {
		if (selectedObjs != null) {
			ArrayList<Node> clonedObjects = new ArrayList<Node>();
			for (Node curObj : selectedObjs) {
				try {
					/*
					 * If the object supports cloning, use it to make another model
					 */
					curObj = curObj.clone();
					clonedObjects.add(curObj);
				} catch (CloneNotSupportedException e) {
					curObj = null;
				}
			}
			
			return clonedObjects;
		} else {
			return null;
		}
	}

	public ArrayList<Point2D> getOffsets() {
		return objectOffsets;
	}
	
}
