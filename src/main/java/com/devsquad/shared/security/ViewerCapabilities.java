package com.devsquad.shared.security;

public record ViewerCapabilities(
        boolean manageHub,
        boolean reviewProposals,
        boolean manageProject,
        boolean manageRecruitment,
        boolean manageBoard,
        boolean apply) {}
