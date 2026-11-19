package com.da.billiards.article;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.da.billiards.article.ArticleDtos.ArticleRequest;
import com.da.billiards.article.ArticleDtos.ArticleResponse;
import com.da.billiards.article.ArticleDtos.ArticleSummary;
import com.da.billiards.common.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping("/api/v1/articles")
    public PageResponse<ArticleSummary> articles(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "9") int size) {
        return articleService.publishedArticles(page, size);
    }

    @GetMapping("/api/v1/articles/{slug}")
    public ArticleResponse article(@PathVariable String slug) {
        return articleService.publishedArticle(slug);
    }

    @GetMapping("/api/v1/admin/articles")
    public PageResponse<ArticleResponse> adminArticles(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return articleService.allArticles(page, size);
    }

    @GetMapping("/api/v1/admin/articles/{id}")
    public ArticleResponse adminArticle(@PathVariable Long id) {
        return articleService.getArticle(id);
    }

    @PostMapping("/api/v1/admin/articles")
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleResponse create(@Valid @RequestBody ArticleRequest request) {
        return articleService.create(request);
    }

    @PutMapping("/api/v1/admin/articles/{id}")
    public ArticleResponse update(@PathVariable Long id, @Valid @RequestBody ArticleRequest request) {
        return articleService.update(id, request);
    }

    @DeleteMapping("/api/v1/admin/articles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        articleService.delete(id);
    }
}
