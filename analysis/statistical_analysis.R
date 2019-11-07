library(lme4)
library(lmerTest)
# library(r2glmm) # R squared
library(MuMIn) # R squared

# setwd("~/Downloads")
data = read.csv("~/Projects/explainable-planning/analysis/data_3qs.csv")
#data = read.csv("~/Projects/explainable-planning/analysis/data_3qs_aligned.csv")
#data = read.csv("~/Projects/explainable-planning/analysis/data_3qs_unaligned.csv")
names(data)

str(data)
data$accuracy = as.factor(data$accuracy)
table(data$accuracy)

## WOW, look at this!
unique(data$group)
boxplot(list(control = data[data$group=="control",]$score, 
             treatment = data[data$group=="experimental",]$score))

boxplot(list(control = as.numeric(data[data$group=="control",]$accuracy), 
             treatment = as.numeric(data[data$group=="experimental",]$accuracy)))


m1 = glmer(accuracy ~ 
            group 
            + (1|participant) 
            + (1|question.ref)
          , family = "binomial"
          , data = data)

summary(m1) # experimental group are exp(1.3352) = 3.8 times more likely to answer correctly!

# R2m: describes the proportion of variance explained by the fixed factor(s) alone
# R2c: describes the proportion of variance explained by both the fixed and random factors
r.squaredGLMM(m1) # goodness of fit of model, without (left) and with (right) random effects

# Doesn't make sense to use ANOVA here since we only have 1 fixed effect
# Instead, report marignal R^2 (R2m) and conditional R^2 (R2c)
# anova(m1)

# Standard error
se_m1 <- sqrt(diag(vcov(m1)))
# Table of estimates with 95% CI
tab_ci_m1 <- cbind(Est = fixef(m1), LL = fixef(m1) - 1.96 * se_m1, UL = fixef(m1) + 1.96 * se_m1)
exp(tab_ci_m1)

table(data$score)

m2 = lmer(score ~ 
             group 
           + (1|participant) 
           + (1|question.ref)
           , data = data)

summary(m2)
r.squaredGLMM(m2)

# Doesn't make sense to use ANOVA here since we only have 1 fixed effect
# Instead, report marignal R^2 (R2m) and conditional R^2 (R2c)
# anova(m2)

# Standard error
se_m2 <- sqrt(diag(vcov(m2)))
# Table of estimates with 95% CI
tab_ci_m2 <- cbind(Est = fixef(m2), LL = fixef(m2) - 1.96 * se_m2, UL = fixef(m2) + 1.96 * se_m2)
tab_ci_m2