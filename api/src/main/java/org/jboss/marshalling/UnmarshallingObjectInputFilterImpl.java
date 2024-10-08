/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.marshalling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * {@link UnmarshallingObjectInputFilter} implementation that is configured by a JEPS 290 style {@code filterSpec}
 * string provided to the constructor. The function returns {@link Status#REJECTED}
 * if the given filterInfo is not acceptable.
 *
 * <p>
 * The {@code filterSpec} string is composed of one or more filter spec elements separated by the {@code ';'} char.
 * A filter spec element that begins with the {@code '!'} char is a 'rejecting element' and indicates resolution of a
 * class name should not be allowed if the rest of the element matches the class name. Otherwise the spec element
 * indicates the class name can be accepted if it matches.
 * </p>
 * <p>
 * Matching is done according to the following rules:
 * <ul>
 *     <li>If the spec element does not terminate in the {@code '*'} char the given class name must match.</li>
 *     <li>If the spec element terminates in the string {@code ".*"} the portion of the class name up to and
 *     including any final {@code '.'} char must match. Such a spec element indicates a single package in which a class
 *     must reside.</li>
 *     <li>If the spec element terminates in the string {@code ".**"} the class name must begin with the portion of the
 *     spec element before the first {@code '*'}. Such a spec element indicates a package hierarchy in which a class
 *     must reside.</li>
 *     <li>Otherwise the spec element ends in the {@code '*'} char and the class name must begin with portion
 *     spec element before the first {@code '*'}. Such a spec element indicates a general string 'starts with' match.</li>
 * </ul>
 * </>
 * <p>
 * The presence of the {@code '='} or {@code '/'} chars anywhere in the filter spec will result in an
 * {@link IllegalArgumentException} from the constructor. The presence of the {@code '*'} char in any substring
 * other than the ones described above will also result in an {@link IllegalArgumentException} from the constructor.
 * </p>
 * <p>
 * If any element in the filter spec indicates a class name should be rejected, it will be rejected. If any element
 * in the filter spec does not begin with the {@code '!'} char, then the filter will act like an allowlist, and
 * at least one non-rejecting filter spec element must match the class name for the filter to return {@code true}.
 * Rejecting elements can be used in an overall filter spec for an allowlist, for example to exclude a particular
 * class from a package that is otherwise allowed.
 * </p>
 *
 * @author Brian Stansberry
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class UnmarshallingObjectInputFilterImpl implements UnmarshallingObjectInputFilter {
    private final List<Function<FilterInfo, Status>> unmarshallingFilters;

    /**
     * Create a filter using the given {@code filterSpec}.
     * @param filterSpec filter configuration as described in the class javadoc. Cannot be {@code null}
     *
     * @throws IllegalArgumentException if the form of {@code filterSpec} violates any of the rules for this class
     */
    UnmarshallingObjectInputFilterImpl(final String filterSpec) {
        if (filterSpec == null) {
            throw new IllegalArgumentException("Parameter 'filterSpec' may not be null");
        }

        List<String> parsedFilterSpecs = new ArrayList<>(Arrays.asList(filterSpec.split(";")));
        unmarshallingFilters = new ArrayList<>(parsedFilterSpecs.size());
        ExactMatchFilter exactMatchAllowlist = null;
        ExactMatchFilter exactMatchDenylist = null;

        for (String spec : parsedFilterSpecs) {

            if (spec.contains("/")) {
                // perhaps this is an attempt to pass a JEPS 290 style limit or module name pattern; not supported
                throw invalidFilterSpec(spec);
            }
            Function<FilterInfo, Status> filter;

            int eqPos = spec.indexOf('=');
            if (eqPos > -1) {
                filter = parseLimitSpec(spec, eqPos);
            } else {

                filter = parseClassSpec(spec, exactMatchDenylist, exactMatchDenylist);
                if ((exactMatchAllowlist == null || exactMatchDenylist == null) && filter instanceof ExactMatchFilter) {
                    ExactMatchFilter exactMatch = (ExactMatchFilter) filter;
                    if (exactMatch.isForAllowlist()) {
                        exactMatchAllowlist = exactMatch;
                    } else {
                        exactMatchDenylist = exactMatch;
                    }
                }
            }
            if (filter != null) {
                unmarshallingFilters.add(filter);
            }
        }

        if (unmarshallingFilters.size() == 0) {
            throw invalidFilterSpec(filterSpec);
        }

    }

    private Function<FilterInfo, Status> parseLimitSpec(String spec, int eqPos) {
        String type = spec.substring(0, eqPos);
        if (eqPos == spec.length() - 1) {
            throw invalidFilterSpec(spec);
        }
        final Function<FilterInfo, Status> filter;
        final long value;
        try {
            value = Long.parseLong(spec.substring(eqPos + 1));
        } catch (NumberFormatException e) {
            throw invalidFilterSpec(spec, e);
        }
        if (value < 0) {
            throw invalidFilterSpec(spec);
        }
        switch (type) {
            case "maxdepth":
                filter = filterInfo -> filterInfo.getDepth() > value ? Status.REJECTED : Status.UNDECIDED;
                break;
            case "maxarray":
                filter = filterInfo -> filterInfo.getArrayLength() > value ? Status.REJECTED : Status.UNDECIDED;
                break;
            case "maxrefs":
                filter = filterInfo -> filterInfo.getReferences() > value ? Status.REJECTED : Status.UNDECIDED;
                break;
            case "maxbytes":
                filter = filterInfo -> filterInfo.getStreamBytes() > value ? Status.REJECTED : Status.UNDECIDED;
                break;
            default:
                throw invalidFilterSpec(spec);
        }
        return filter;
    }

    private Function<FilterInfo, Status> parseClassSpec(String spec,
                                                        ExactMatchFilter exactMatchDenylist,
                                                        ExactMatchFilter exactMatchAllowlist) {
        Function<FilterInfo, Status> filter = null;
        boolean denylistElement = spec.startsWith("!");

        // For a denylist element, return FALSE for a match; i.e. don't resolve
        // For an allowlist, return TRUE for a match; i.e. definitely do resolve
        // For any non-match, return null which means that check has no opinion
        final Status matchReturn = denylistElement ? Status.REJECTED : Status.ALLOWED;

        if (denylistElement) {
            if (spec.length() == 1) {
                throw invalidFilterSpec(spec);
            }
            spec = spec.substring(1);
        }

        int lastStar = spec.lastIndexOf('*');
        if (lastStar >= 0) {
            if (lastStar != spec.length() - 1) {
                // wildcards only allowed at the end
                throw invalidFilterSpec(spec);
            }
            int firstStar = spec.indexOf('*');
            if (firstStar != lastStar) {
                if (firstStar == lastStar - 1 && spec.endsWith(".**")) {
                    if (spec.length() == 3) {
                        throw invalidFilterSpec(spec);
                    }
                    String pkg = spec.substring(0, spec.length() - 2);
                    filter = filterInfo -> classNameFor(filterInfo).startsWith(pkg) ? matchReturn : Status.UNDECIDED;
                } else {
                    // there's an extra star in some spot other than between a final '.' and '*'
                    throw invalidFilterSpec(spec);
                }
            } else if (spec.endsWith(".*")) {
                if (spec.length() == 2) {
                    throw invalidFilterSpec(spec);
                }
                String pkg = spec.substring(0, spec.length() - 1);
                filter = filterInfo -> classNameFor(filterInfo).startsWith(pkg) && classNameFor(filterInfo).lastIndexOf('.') == pkg.length() - 1 ? matchReturn : Status.UNDECIDED;
            } else {
                String startsWith = spec.substring(0, spec.length() - 1); // note that an empty 'startsWith' is ok; e.g. from a "*" spec to allow all
                filter = filterInfo -> classNameFor(filterInfo).startsWith(startsWith) ? matchReturn : Status.UNDECIDED;
            }
        } else {
            // For exact matches store them in a set and just do a single set.contains check
            if (denylistElement) {
                if (exactMatchDenylist == null) {
                    filter = exactMatchDenylist = new ExactMatchFilter(false);
                }
                exactMatchDenylist.addMatchingClass(spec);
            } else {
                if (exactMatchAllowlist == null) {
                    filter = exactMatchAllowlist = new ExactMatchFilter(true);
                }
                exactMatchAllowlist.addMatchingClass(spec);
            }
        }
        return filter;
    }

    @Override
    public Status checkInput(FilterInfo filterInfo) {

        for (Function<FilterInfo, Status> func : unmarshallingFilters) {
            Status status = func.apply(filterInfo);
            if (status != Status.UNDECIDED) {
                return status;
            }
        }
        return Status.UNDECIDED;
    }

    private static String classNameFor(FilterInfo filterInfo) {
        if (filterInfo.getUnmarshalledClass() == null) {
            return "";
        }
        return filterInfo.getUnmarshalledClass().getName();
    }

    private static IllegalArgumentException invalidFilterSpec(String spec) {
        return invalidFilterSpec(spec, null);
    }

    private static IllegalArgumentException invalidFilterSpec(String spec, Throwable cause) {
        return new IllegalArgumentException(String.format("Invalid unmarshalling filter specification '%s'", spec), cause);
    }

    private static class ExactMatchFilter implements Function<FilterInfo, Status> {
        private final Set<String> matches = new HashSet<>();
        private final Status matchResult;

        private ExactMatchFilter(boolean forAllowlist) {
            this.matchResult = forAllowlist ? Status.ALLOWED : Status.REJECTED;
        }

        private void addMatchingClass(String name) {
            matches.add(name);
        }

        private boolean isForAllowlist() {
            return matchResult == Status.ALLOWED;
        }

        @Override
        public Status apply(FilterInfo filterInfo) {
            return matches.contains(classNameFor(filterInfo)) ? matchResult : Status.UNDECIDED;
        }
    }
}
