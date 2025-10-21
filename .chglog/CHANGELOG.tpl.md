<!-- changelog is partially generated, so it doesn't follow headings and required structure, so we disable it. -->
<!-- markdownlint-disable -->

{{ if .Versions -}}
<a name="unreleased"></a>
# Unreleased

{{ if .Unreleased.CommitGroups -}}
{{ range .Unreleased.CommitGroups -}}
## {{ .Title }}

{{ range .Commits -}}
{{ if and (not (hasPrefix .Subject "changelog rebuild")) (not (hasPrefix .Subject "layer docs update")) (not (hasPrefix .Subject "bump version to")) -}}
* {{ if .Scope }}**{{ .Scope }}:** {{ end }}{{ .Subject }}
{{ end -}}
{{ end }}
{{ end -}}
{{ end -}}
{{ end -}}

{{ range .Versions }}
<a name="{{ .Tag.Name }}"></a>
## {{ if .Tag.Previous }}[{{ .Tag.Name }}]{{ else }}{{ .Tag.Name }}{{ end }} - {{ datetime "2006-01-02" .Tag.Date }}
{{ range .CommitGroups -}}

## {{ .Title }}

{{ range .Commits -}}
{{ if and (not (hasPrefix .Subject "changelog rebuild")) (not (hasPrefix .Subject "layer docs update")) (not (hasPrefix .Subject "bump version to")) -}}
* {{ if .Scope }}**{{ .Scope }}:** {{ end }}{{ .Subject }}
{{ end -}}
{{ end }}
{{ end -}}

{{- if .RevertCommits -}}
## Reverts
{{ range .RevertCommits -}}
* {{ .Revert.Header }}
{{ end }}
{{ end -}}

{{- if .MergeCommits -}}
## Pull Requests

{{ range .MergeCommits -}}
* {{ .Header }}
{{ end }}
{{ end -}}

{{- if .NoteGroups -}}
{{ range .NoteGroups -}}
## {{ .Title }}
{{ range .Notes }}
{{ .Body }}
{{ end }}
{{ end -}}
{{ end -}}
{{ end -}}

{{- if .Versions }}
[Unreleased]: {{ .Info.RepositoryURL }}/compare/{{ $latest := index .Versions 0 }}{{ $latest.Tag.Name }}...HEAD
{{ range .Versions -}}
{{ if .Tag.Previous -}}
[{{ .Tag.Name }}]: {{ $.Info.RepositoryURL }}/compare/{{ .Tag.Previous.Name }}...{{ .Tag.Name }}
{{ end -}}
{{ end -}}
{{ end -}}
