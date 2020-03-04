package models.explanation;

public enum HPolicyTag {
	BETTER_TARGET_QA, // HPolicy improves the queried QA
	WORSE_TARGET_QA_NON_DOMINATED, // HPolicy worsens the queried QA, but improves some other QA(s)
	SAME_TARGET_QA_NON_DOMINATED, // HPolicy doesn't affect the queried QA, and HPolicy is non-dominated
	WORSE_TARGET_QA_DOMINATED, // HPolicy worsens the queried QA, and doesn't improve any other QA
	SAME_TARGET_QA_DOMINATED, // HPolicy doesn't affect the queried QA, but worsens some other QA(s)
	EQUIVALENT // HPolicy doesn't affect any QA
}
