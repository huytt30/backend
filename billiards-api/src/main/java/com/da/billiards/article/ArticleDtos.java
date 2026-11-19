package com.da.billiards.article;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ArticleDtos {

    private ArticleDtos() {
    }

    public record ArticleRequest(
            @NotBlank(message = "Vui lòng nhập tiêu đề") @Size(max = 200) String title,
            @Size(max = 500) String summary,
            @NotBlank(message = "Vui lòng nhập nội dung") @Size(max = 50000) String content,
            @Size(max = 500) String thumbnail,
            boolean published) {
    }

    public record ArticleSummary(Long id, String title, String slug, String summary, String thumbnail,
                                 LocalDateTime publishedAt) {

        static ArticleSummary from(Article article) {
            return new ArticleSummary(article.getId(), article.getTitle(), article.getSlug(), article.getSummary(),
                    article.getThumbnail(), article.getPublishedAt());
        }
    }

    public record ArticleResponse(Long id, String title, String slug, String summary, String content,
                                  String thumbnail, boolean published, LocalDateTime publishedAt,
                                  LocalDateTime createdAt) {

        static ArticleResponse from(Article article) {
            return new ArticleResponse(article.getId(), article.getTitle(), article.getSlug(), article.getSummary(),
                    article.getContent(), article.getThumbnail(), article.isPublished(), article.getPublishedAt(),
                    article.getCreatedAt());
        }
    }
}
