import { TemplateMaker } from "@/components/template-maker";

export default async function TemplatePage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return <TemplateMaker templateId={id} />;
}
