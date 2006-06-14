package org.spearce.egit.ui.internal.decorators;

import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.spearce.egit.core.project.GitProjectData;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIIcons;
import org.spearce.egit.ui.UIText;
import org.spearce.jgit.lib.MergedTree;
import org.spearce.jgit.lib.TreeEntry;

public class GitResourceDecorator extends LabelProvider
    implements
        ILightweightLabelDecorator
{
    private static IResource toIResource(final Object e)
    {
        if (e instanceof IResource)
        {
            return (IResource) e;
        }
        else if (e instanceof IAdaptable)
        {
            final Object c = ((IAdaptable) e).getAdapter(IResource.class);
            if (c instanceof IResource)
            {
                return (IResource) c;
            }
        }
        return null;
    }

    public void decorate(final Object element, final IDecoration decoration)
    {
        final IResource rsrc = toIResource(element);
        if (rsrc == null)
        {
            return;
        }

        final GitProjectData d = GitProjectData.getDataFor(rsrc.getProject());
        if (d == null)
        {
            return;
        }

        RepositoryMapping mapped = d.getRepositoryMapping(rsrc);
        if (mapped != null)
        {
            decoration.addSuffix(" [GIT]");
            decoration.addOverlay(UIIcons.OVR_SHARED);
            return;
        }

        TreeEntry[] n;
        try
        {
            n = d.getActiveDiffTreeEntries(rsrc);
        }
        catch (IOException ioe)
        {
            // If we throw an exception Eclipse will log the error and
            // unregister us thereby preventing us from dragging down the
            // entire workbench because we are crashing.
            //
            throw new RuntimeException(UIText.Decorator_failedLazyLoading, ioe);
        }

        if (n != null)
        {
            if (MergedTree.isAdded(n))
            {
                decoration.addOverlay(UIIcons.OVR_PENDING_ADD);
            }
            else if (MergedTree.isRemoved(n))
            {
                decoration.addOverlay(UIIcons.OVR_PENDING_REMOVE);
            }
            else if (MergedTree.isModified(n))
            {
                decoration.addPrefix(">");
                decoration.addOverlay(UIIcons.OVR_SHARED);
            }
            else
            {
                decoration.addOverlay(UIIcons.OVR_SHARED);
            }
        }
        else
        {
            decoration.addSuffix(" (untracked)");
        }
    }
}