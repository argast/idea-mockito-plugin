package com.github.argast;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiJavaFile;

public class MockitoPluginUtils {

    public static boolean containsMockImport(PsiJavaFile javaFile) {
        PsiImportList importList = javaFile.getImportList();
        importList.findSingleImportStatement("mock");


        Module module = ProjectRootManager.getInstance(javaFile.getProject()).getFileIndex().getModuleForFile(javaFile.getVirtualFile());

        OrderEntry[] orderEntries = ModuleRootManager.getInstance(module).getOrderEntries();
        // Select libraries if any
        for (int i = 0; i <= orderEntries.length - 1; i++) {
            if ((orderEntries[i] instanceof LibraryOrderEntry))  {
                LibraryOrderEntry libraryEntry = (LibraryOrderEntry) orderEntries[i];
                System.out.println(orderEntries[i].getPresentableName() + " | library level: " +
                        libraryEntry.getLibraryLevel());
            }
        }
        return false;
    }
}
