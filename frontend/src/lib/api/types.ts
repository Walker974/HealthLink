/** Java response records serialize their properties; nullable relationships remain explicit. */
export type ApiModel<T, Nullable extends keyof T = never> = Omit<Required<T>, Nullable> & {
  [Key in Nullable]-?: Exclude<T[Key], undefined> | null
}
