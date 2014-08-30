package com.example.cropimage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public interface FileAccessable {

    public interface FileAccessableFilter {
        boolean accept(FileAccessable file);
    }

    FileAccessable createBySubpath(String subpath);
    FileAccessable createByExtension(String extensionName);
    boolean exists();
    boolean isFile();
    boolean isDirectory();
    String getName();
    List<FileAccessable> list();
    List<FileAccessable> list(FileAccessableFilter filter);
    InputStream getInputStream();

    static public class Factory {

        static private HashMap<String, WeakReference<ZipFile>> sZipFiles = new HashMap<String, WeakReference<ZipFile>>();

        static public FileAccessable create(String root, String subpath) throws IOException {
            if (new File(root).isDirectory()) {
                return new DeskFile(root, subpath);
            } else {
                ZipFile zipFile;
                synchronized (sZipFiles) {
                    WeakReference<ZipFile> zipFileRef = sZipFiles.get(root);
                    zipFile = zipFileRef == null ? null : zipFileRef.get();
                    if (zipFile == null) {
                        zipFile = new ZipFile(root);
                        sZipFiles.put(root, new WeakReference<ZipFile>(zipFile));
                    }
                }
                return new ZipInnerFile(zipFile, subpath);
            }
        }

        static public void clearCache() {
            synchronized (sZipFiles) {
                sZipFiles.clear();
            }
        }
    }

    static abstract class AbstractFileAccessable implements FileAccessable {

        @Override
        public boolean isDirectory() {
            if (!exists()) return false;
            if (isFile()) return false;
            return true;
        }

        @Override
        public List<FileAccessable> list(FileAccessableFilter filter) {
            if (filter == null) return list();

            List<FileAccessable> allFiles = list();
            if (allFiles == null) return null;

            List<FileAccessable> returnFiles = new ArrayList<FileAccessable>();
            for (FileAccessable fileAccessable : allFiles) {
                if (filter.accept(fileAccessable)) {
                    returnFiles.add(fileAccessable);
                }
            }
            return returnFiles;
        }
    }

    static class DeskFile extends AbstractFileAccessable {

        File mFile;

        public DeskFile(File root, String subpath) {
            mFile = new File(root, subpath);
        }

        public DeskFile(String root, String subpath) {
            mFile = new File(root, subpath);
        }

        public DeskFile(String filePath) {
            mFile = new File(filePath);
        }

        public File getFile() {
            return mFile;
        }

        @Override
        public boolean exists() {
            return mFile.exists();
        }

        @Override
        public boolean isFile() {
            return mFile.isFile();
        }

        @Override
        public List<FileAccessable> list() {
            String[] files = mFile.list();
            List<FileAccessable> returnList = new ArrayList<FileAccessable>();
            for (String file : files) {
                returnList.add(new DeskFile(mFile, file));
            }
            return returnList;
        }

        @Override
        public InputStream getInputStream() {
            try {
                return new FileInputStream(mFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public String getName() {
            return mFile.getName();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof DeskFile)) return false;

            DeskFile target = (DeskFile) o;
            if (!mFile.equals(target.mFile)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return mFile.hashCode();
        }

        @Override
        public FileAccessable createBySubpath(String subpath) {
            return new DeskFile(mFile.getAbsolutePath(), subpath);
        }

        @Override
        public FileAccessable createByExtension(String extensionName) {
            return new DeskFile(mFile.getAbsolutePath() + extensionName);
        }
    }

    static class ZipInnerFile extends AbstractFileAccessable {

        ZipFile mZipFile;
        String mEntryName;
        boolean mIsFolder;
        boolean mExists;

        public ZipInnerFile(ZipFile zipFile, String entryName) {
            init(zipFile, entryName);
        }

        private void init(ZipFile zipFile, String entryName) {
            mZipFile = zipFile;
            mEntryName = entryName.endsWith("/") ? entryName.substring(0, entryName.length()-1) : entryName;

            if (mZipFile == null) {
                return;
            }

            ZipEntry zipEntry = zipFile.getEntry(entryName);
            if (zipEntry == null) {
                String folder = entryName;
                if (!folder.endsWith("/")) folder += "/";

                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) entries.nextElement();
                    if (entry.getName().startsWith(folder)) {
                        mExists = true;
                        mIsFolder = true;
                        break;
                    }
                }
            } else {
                mExists = true;
                mIsFolder = zipEntry.isDirectory();
            }
        }

        @Override
        public boolean exists() {
            return mExists;
        }

        @Override
        public boolean isFile() {
            return !mIsFolder;
        }

        @Override
        public List<FileAccessable> list() {
            if (!mExists || !mIsFolder) return null;

            Enumeration<? extends ZipEntry> entries = mZipFile.entries();
            List<FileAccessable> returnList = new ArrayList<FileAccessable>();
            HashSet<String> setForQuickCheck = new HashSet<String>();

            while (entries.hasMoreElements()) {
                String currEntryFolder = mEntryName + '/';
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.getName().length() > currEntryFolder.length() && entry.getName().startsWith(currEntryFolder)) {
                    String subpath = entry.getName().substring(currEntryFolder.length());
                    String name = entry.getName();
                    int index = subpath.indexOf('/');
                    if (index != -1) {
                        String folderName = subpath.substring(0, index);
                        name = currEntryFolder + folderName;
                    }

                    if (!setForQuickCheck.contains(name)) {
                        returnList.add(new ZipInnerFile(mZipFile, name));
                        setForQuickCheck.add(name);
                    }
                }
            }
            return returnList;
        }

        @Override
        public InputStream getInputStream() {
            if (!mExists || mIsFolder) return null;
            ZipEntry zipEntry = mZipFile.getEntry(mEntryName);
            try {
                return mZipFile.getInputStream(zipEntry);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public String getName() {
            int separatorIndex = mEntryName.lastIndexOf('/');
            return (separatorIndex < 0) ? mEntryName : mEntryName.substring(separatorIndex + 1, mEntryName.length());
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof ZipInnerFile)) return false;

            ZipInnerFile target = (ZipInnerFile) o;
            if (!objectEquals(this.mZipFile, target.mZipFile)) return false;
            if (!objectEquals(this.mEntryName, target.mEntryName)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            if (mZipFile == null) return mEntryName.hashCode();
            return mZipFile.hashCode() ^ mEntryName.hashCode();
        }

        static private boolean objectEquals(Object obj1, Object obj2) {
            if (obj1 == obj2) return true;
            if (obj1 == null) return false;
            return obj1.equals(obj2);
        }

        @Override
        public FileAccessable createBySubpath(String subpath) {
            return new ZipInnerFile(mZipFile, mEntryName + "/" + subpath);
        }

        @Override
        public FileAccessable createByExtension(String extensionName) {
            return new ZipInnerFile(mZipFile, mEntryName + extensionName);
        }
    }
}
