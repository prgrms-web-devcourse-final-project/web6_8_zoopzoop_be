package org.tuna.zoopzoop.backend.domain.news.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.archive.folder.service.PersonalArchiveFolderService;
import org.tuna.zoopzoop.backend.domain.datasource.dto.FileSummary;
import org.tuna.zoopzoop.backend.domain.datasource.dto.FolderFilesDto;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsService {
    private final PersonalArchiveFolderService folderService;

    public List<String> getTagFrequencyFromFiles(Integer memberId, Integer folderId) {
        FolderFilesDto folderFilesDto = folderService.getFilesInFolder(memberId, folderId);

        List<FileSummary> files = folderFilesDto.files();

        Map<String, Long> tags = files.stream()
                .flatMap(file -> {
                    List<String> ts = file.tags();
                    return (ts == null ? List.<String>of() : ts).stream();
                })
                .collect(Collectors.groupingBy(
                        tagName -> tagName,
                        Collectors.counting()
                ));

        List<String> frequency = tags.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        return frequency;
    }
}
