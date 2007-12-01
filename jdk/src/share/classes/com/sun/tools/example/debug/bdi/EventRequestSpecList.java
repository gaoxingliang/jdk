/*
 * Copyright 1999 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.example.debug.bdi;

import com.sun.jdi.*;
import com.sun.jdi.request.*;

import java.util.*;

class EventRequestSpecList {

    // all specs
    private List<EventRequestSpec> eventRequestSpecs = Collections.synchronizedList(
                                                  new ArrayList<EventRequestSpec>());

    final ExecutionManager runtime;

    EventRequestSpecList(ExecutionManager runtime) {
        this.runtime = runtime;
    }

    /**
     * Resolve all deferred eventRequests waiting for 'refType'.
     */
    void resolve(ReferenceType refType) {
        synchronized(eventRequestSpecs) {
            Iterator iter = eventRequestSpecs.iterator();
            while (iter.hasNext()) {
                ((EventRequestSpec)iter.next()).attemptResolve(refType);
             }
        }
    }

    void install(EventRequestSpec ers, VirtualMachine vm) {
        synchronized (eventRequestSpecs) {
            eventRequestSpecs.add(ers);
        }
        if (vm != null) {
            ers.attemptImmediateResolve(vm);
        }
    }

    BreakpointSpec
    createClassLineBreakpoint(String classPattern, int line) {
        ReferenceTypeSpec refSpec =
            new PatternReferenceTypeSpec(classPattern);
        return new LineBreakpointSpec(this, refSpec, line);
    }

    BreakpointSpec
    createSourceLineBreakpoint(String sourceName, int line) {
        ReferenceTypeSpec refSpec =
            new SourceNameReferenceTypeSpec(sourceName, line);
        return new LineBreakpointSpec(this, refSpec, line);
    }

    BreakpointSpec
    createMethodBreakpoint(String classPattern,
                           String methodId, List methodArgs) {
        ReferenceTypeSpec refSpec =
            new PatternReferenceTypeSpec(classPattern);
        return new MethodBreakpointSpec(this, refSpec,
                                        methodId, methodArgs);
    }

    ExceptionSpec
    createExceptionIntercept(String classPattern,
                             boolean notifyCaught,
                             boolean notifyUncaught) {
        ReferenceTypeSpec refSpec =
            new PatternReferenceTypeSpec(classPattern);
        return new ExceptionSpec(this, refSpec,
                                 notifyCaught, notifyUncaught);
    }

    AccessWatchpointSpec
    createAccessWatchpoint(String classPattern, String fieldId) {
        ReferenceTypeSpec refSpec =
            new PatternReferenceTypeSpec(classPattern);
        return new AccessWatchpointSpec(this, refSpec, fieldId);
    }

    ModificationWatchpointSpec
    createModificationWatchpoint(String classPattern, String fieldId) {
        ReferenceTypeSpec refSpec =
            new PatternReferenceTypeSpec(classPattern);
        return new ModificationWatchpointSpec(this, refSpec, fieldId);
    }

    void delete(EventRequestSpec ers) {
        EventRequest request = ers.getEventRequest();
        synchronized (eventRequestSpecs) {
            eventRequestSpecs.remove(ers);
        }
        if (request != null) {
            request.virtualMachine().eventRequestManager()
                .deleteEventRequest(request);
        }
        notifyDeleted(ers);
        //### notify delete - here?
    }

    List<EventRequestSpec> eventRequestSpecs() {
        // We need to make a copy to avoid synchronization problems
        synchronized (eventRequestSpecs) {
            return new ArrayList<EventRequestSpec>(eventRequestSpecs);
        }
    }

    // --------  notify routines --------------------

    private Vector specListeners() {
        return (Vector)runtime.specListeners.clone();
    }

    void notifySet(EventRequestSpec spec) {
        Vector l = specListeners();
        SpecEvent evt = new SpecEvent(spec);
        for (int i = 0; i < l.size(); i++) {
            spec.notifySet((SpecListener)l.elementAt(i), evt);
        }
    }

    void notifyDeferred(EventRequestSpec spec) {
        Vector l = specListeners();
        SpecEvent evt = new SpecEvent(spec);
        for (int i = 0; i < l.size(); i++) {
            spec.notifyDeferred((SpecListener)l.elementAt(i), evt);
        }
    }

    void notifyDeleted(EventRequestSpec spec) {
        Vector l = specListeners();
        SpecEvent evt = new SpecEvent(spec);
        for (int i = 0; i < l.size(); i++) {
            spec.notifyDeleted((SpecListener)l.elementAt(i), evt);
        }
    }

    void notifyResolved(EventRequestSpec spec) {
        Vector l = specListeners();
        SpecEvent evt = new SpecEvent(spec);
        for (int i = 0; i < l.size(); i++) {
            spec.notifyResolved((SpecListener)l.elementAt(i), evt);
        }
    }

    void notifyError(EventRequestSpec spec, Exception exc) {
        Vector l = specListeners();
        SpecErrorEvent evt = new SpecErrorEvent(spec, exc);
        for (int i = 0; i < l.size(); i++) {
            spec.notifyError((SpecListener)l.elementAt(i), evt);
        }
    }
}
