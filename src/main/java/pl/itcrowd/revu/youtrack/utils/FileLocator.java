package pl.itcrowd.revu.youtrack.utils;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.text.StrTokenizer;

import java.io.File;
import java.net.MalformedURLException;

public final class FileLocator {
// -------------------------- STATIC METHODS --------------------------

    public static VirtualFile findRelativeFile(String path, VirtualFile base)
    {
        if (!base.isDirectory()) {
            base = base.getParent();
        }
        File file = new File(base.getCanonicalPath());

        path = path.replace('\\', '/');
        final StrTokenizer tokenizer = new StrTokenizer(path, '/');

        while (tokenizer.hasNext()) {
            if (!file.isDirectory()) {
                break;
            }
            boolean childFound = false;
            final String name = tokenizer.nextToken().toLowerCase();
            final File[] files = file.listFiles();
            if (files == null) {
                break;
            }
            for (File child : files) {
                if (child.getName().equalsIgnoreCase(name)) {
                    file = child;
                    childFound = true;
                    break;
                }
            }
            if (!childFound) {
                return null;
            }
        }
        try {
            return VfsUtil.findFileByURL(file.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private FileLocator()
    {
    }
}
