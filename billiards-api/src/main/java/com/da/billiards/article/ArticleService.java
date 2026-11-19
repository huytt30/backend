package com.da.billiards.article;

import java.time.LocalDateTime;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.da.billiards.article.ArticleDtos.ArticleRequest;
import com.da.billiards.article.ArticleDtos.ArticleResponse;
import com.da.billiards.article.ArticleDtos.ArticleSummary;
import com.da.billiards.common.ApiException;
import com.da.billiards.common.PageResponse;
import com.da.billiards.common.Pages;
import com.da.billiards.common.SlugUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ArticleService {

    private final ArticleRepository articleRepository;

    @Transactional(readOnly = true)
    public PageResponse<ArticleSummary> publishedArticles(int page, int size) {
        var pageable = Pages.of(page, size, Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id")));
        return PageResponse.of(articleRepository.findByPublishedTrue(pageable), ArticleSummary::from);
    }

    @Transactional(readOnly = true)
    public ArticleResponse publishedArticle(String slug) {
        return articleRepository.findBySlugAndPublishedTrue(slug).map(ArticleResponse::from)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy bài viết"));
    }

    @Transactional(readOnly = true)
    public PageResponse<ArticleResponse> allArticles(int page, int size) {
        var pageable = Pages.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return PageResponse.of(articleRepository.findAll(pageable), ArticleResponse::from);
    }

    @Transactional(readOnly = true)
    public ArticleResponse getArticle(Long id) {
        return ArticleResponse.from(findArticle(id));
    }

    public ArticleResponse create(ArticleRequest request) {
        Article article = new Article();
        article.setSlug(SlugUtil.unique(request.title(), articleRepository::existsBySlug));
        apply(article, request);
        return ArticleResponse.from(articleRepository.save(article));
    }

    public ArticleResponse update(Long id, ArticleRequest request) {
        Article article = findArticle(id);
        if (!article.getTitle().equals(request.title().trim())) {
            article.setSlug(SlugUtil.unique(request.title(), slug -> articleRepository.existsBySlugAndIdNot(slug, id)));
        }
        apply(article, request);
        return ArticleResponse.from(article);
    }

    public void delete(Long id) {
        articleRepository.delete(findArticle(id));
    }

    private void apply(Article article, ArticleRequest request) {
        article.setTitle(request.title().trim());
        article.setSummary(request.summary() == null ? null : request.summary().trim());
        article.setContent(request.content().trim());
        article.setThumbnail(request.thumbnail() == null || request.thumbnail().isBlank() ? null : request.thumbnail().trim());
        // Ghi nhận thời điểm đăng ở lần đầu tiên bài viết được công khai
        if (request.published() && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        article.setPublished(request.published());
    }

    private Article findArticle(Long id) {
        return articleRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy bài viết"));
    }
}
