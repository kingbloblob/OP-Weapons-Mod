package com.onepiecearsenal.test;

import java.util.List;

import net.minecraft.gametest.framework.GameTestHelper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.onepiecearsenal.guide.GuideChapters;

/**
 * The Shipwright's Manual: every chapter in arsenal/guide.json loads, has an icon and some text, and its
 * generated pages are ones the manual knows how to write. Run with ./gradlew runGameTest.
 */
public class GuideTests {
	@GameTest
	public void everyChapterHasPages(GameTestHelper helper) {
		List<GuideChapters.Chapter> chapters = GuideChapters.chapters();
		helper.assertTrue(chapters.size() >= 10, "guide.json should have its chapters, found " + chapters.size());
		for (GuideChapters.Chapter chapter : chapters) {
			helper.assertTrue(!(chapter.iconStack().isEmpty()), chapter.id + " should have an icon");
			List<String> lines = chapter.lines();
			helper.assertTrue(!(lines.isEmpty()), chapter.id + " should have text");
			for (String line : lines) {
				helper.assertTrue(!(line.contains("isn't a page the manual knows")), chapter.id + ": " + line);
			}
		}
		helper.succeed();
	}
}
